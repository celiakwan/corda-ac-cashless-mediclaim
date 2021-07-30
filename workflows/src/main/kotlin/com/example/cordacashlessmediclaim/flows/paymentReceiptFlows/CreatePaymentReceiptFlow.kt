package com.example.cordacashlessmediclaim.flows.paymentReceiptFlows

import co.paralleluniverse.fibers.Suspendable
import com.example.cordacashlessmediclaim.contracts.PaymentReceiptContract
import com.example.cordacashlessmediclaim.data.Account
import com.example.cordacashlessmediclaim.data.AccountRole
import com.example.cordacashlessmediclaim.flows.AccountIntegration
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.cordacashlessmediclaim.states.PaymentReceiptState
import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.r3.corda.lib.ci.workflows.SyncKeyMappingInitiator
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

@InitiatingFlow
@StartableByRPC
class CreatePaymentReceiptFlow(
        private val payer: AccountRole,
        private val payee: AccountRole,
        private val currency: Currency,
        private val amount: BigDecimal,
        private val preAuthorizationLinearId: UniqueIdentifier
): AccountIntegration<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val payerAccountInfo = getAccountInfo(payer.name)

        payerAccountInfo.apply {
            if (host != ourIdentity) {
                throw FlowException("payer host doesn't match our identity")
            }
        }

        val payerAccountKey = newKeyFor(payerAccountInfo)
        val payerAccount = AnonymousParty(payerAccountKey)
        val payeeAccountInfo = getAccountInfo(payee.name)
        val payeeAccount = requestKeyFor(payeeAccountInfo)
        subFlow(SyncKeyMappingInitiator(payeeAccountInfo.host, listOf(payerAccount)))

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(preAuthorizationLinearId))
        val preAuthorizationStateAndRef = serviceHub.vaultService.queryBy<PreAuthorizationState>(queryCriteria).states
                .singleOrNull() ?: throw FlowException("PreAuthorizationState with linear ID $preAuthorizationLinearId not found")

        preAuthorizationStateAndRef.state.data.apply {
            val policyIssuerAccountInfo = getAccountInfo(policyIssuerAccount.role.name)
            if (payerAccountInfo.host != policyIssuerAccountInfo.host) {
                throw FlowException("payer host doesn't match policyIssuerAccount host in preAuthorizationState")
            }
            val providerAccountInfo = getAccountInfo(providerAccount.role.name)
            if (payeeAccountInfo.host != providerAccountInfo.host) {
                throw FlowException("payee host doesn't match providerAccount host in preAuthorizationState")
            }
        }

        val outputState = PaymentReceiptState(
                payerAccount = Account(
                        party = payerAccount,
                        role = payer
                ),
                payeeAccount = Account(
                        party = payeeAccount,
                        role = payee
                ),
                currency = currency,
                amount = amount,
                submissionTime = Instant.now(),
                preAuthorizationLinearId = preAuthorizationLinearId,
                status = PaymentReceiptStatus.CREATED
        )
        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(outputState)
                .addReferenceState(ReferencedStateAndRef(preAuthorizationStateAndRef))
                .addCommand(PaymentReceiptContract.Commands.Create(), payerAccountKey, payeeAccount.owningKey)

        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder, payerAccountKey)
        val session = initiateFlow(payeeAccountInfo.host)
        val fullSignedTransaction = subFlow(CollectSignaturesFlow(selfSignedTransaction, listOf(session),
                listOf(payerAccountKey)))
        return subFlow(FinalityFlow(fullSignedTransaction, session))
    }
}

@InitiatedBy(CreatePaymentReceiptFlow::class)
class CreatePaymentReceiptResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is PaymentReceiptContract.Commands.Create) {
                    throw FlowException("Only PaymentReceiptContract.Commands.Create command is allowed")
                }

                val outputState = stx.tx.outputStates.single() as PaymentReceiptState
                outputState.apply {
                    if (!serviceHub.keyManagementService.keys.contains(payeeAccount.party.owningKey)) {
                        throw FlowException("payeeAccount doesn't match our identity")
                    }
                    if (payerAccount.party.owningKey != stx.sigs.single().by) {
                        throw FlowException("payerAccount doesn't match the initial signer's identity")
                    }
                }
            }
        })

        return subFlow(ReceiveFinalityFlow(session))
    }
}