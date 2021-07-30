package com.example.cordacashlessmediclaim.flows.preAuthorizationFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.cordacashlessmediclaim.contracts.PreAuthorizationContract
import com.example.cordacashlessmediclaim.data.Account
import com.example.cordacashlessmediclaim.flows.AccountIntegration
import com.example.cordacashlessmediclaim.data.AccountRole
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import com.r3.corda.lib.ci.workflows.SyncKeyMappingInitiator
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
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant
import java.util.Currency

@InitiatingFlow
@StartableByRPC
class CreatePreAuthorizationFlow(
        private val policyHolder: Party,
        private val membershipNumber: String,
        private val provider: AccountRole,
        private val diagnosisDescription: String,
        private val currency: Currency,
        private val amount: BigDecimal,
        private val policyIssuer: AccountRole
): AccountIntegration<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val providerAccountInfo = getAccountInfo(provider.name)

        providerAccountInfo.apply {
            if (host != ourIdentity) {
                throw FlowException("provider host doesn't match our identity")
            }
        }

        val providerAccountKey = newKeyFor(providerAccountInfo)
        val providerAccount = AnonymousParty(providerAccountKey)
        val policyIssuerAccountInfo = getAccountInfo(policyIssuer.name)
        val policyIssuerAccount = requestKeyFor(policyIssuerAccountInfo)
        subFlow(SyncKeyMappingInitiator(policyIssuerAccountInfo.host, listOf(providerAccount)))

        val outputState = PreAuthorizationState(
                policyHolder = policyHolder,
                membershipNumber = membershipNumber,
                providerAccount = Account(
                        party = providerAccount,
                        role = provider
                ),
                diagnosisDescription = diagnosisDescription,
                currency = currency,
                amount = amount,
                policyIssuerAccount = Account(
                        party = policyIssuerAccount,
                        role = policyIssuer
                ),
                submissionTime = Instant.now(),
                status = PreAuthorizationStatus.CREATED
        )
        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(outputState)
                .addCommand(PreAuthorizationContract.Commands.Create(), providerAccountKey, policyHolder.owningKey)

        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder, providerAccountKey)
        val fullSignedTransaction = subFlow(CollectSignaturesInitiatingFlow(selfSignedTransaction, listOf(policyHolder),
                listOf(providerAccountKey)))
        val sessions = listOf(policyIssuerAccountInfo.host, policyHolder).map { initiateFlow(it) }
        return subFlow(FinalityFlow(fullSignedTransaction, sessions))
    }
}

@InitiatingFlow
class CollectSignaturesInitiatingFlow(
        private val transaction: SignedTransaction,
        private val signers: List<Party>,
        private val initiatorKeys: List<PublicKey>
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val sessions = signers.map { initiateFlow(it) }
        return subFlow(CollectSignaturesFlow(transaction, sessions, initiatorKeys))
    }
}

@InitiatedBy(CollectSignaturesInitiatingFlow::class)
class CollectSignaturesResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(object: SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is PreAuthorizationContract.Commands.Create) {
                    throw FlowException("Only PreAuthorizationStateContract.Commands.Create command is allowed")
                }

                val outputState = stx.tx.outputStates.single() as PreAuthorizationState
                outputState.apply {
                    if (policyHolder != ourIdentity) {
                        throw FlowException("policyHolder doesn't match our identity")
                    }
                    if (providerAccount.party.owningKey != stx.sigs.single().by) {
                        throw FlowException("providerAccount doesn't match the initial signer's identity")
                    }
                }
            }
        })
    }
}

@InitiatedBy(CreatePreAuthorizationFlow::class)
class CreatePreAuthorizationResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction = subFlow(ReceiveFinalityFlow(session))
}