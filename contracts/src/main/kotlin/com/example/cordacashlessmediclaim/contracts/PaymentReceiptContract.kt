package com.example.cordacashlessmediclaim.contracts

import com.example.cordacashlessmediclaim.contracts.verification.Transition as TS
import com.example.cordacashlessmediclaim.contracts.verification.Verifier
import com.example.cordacashlessmediclaim.states.PartyRole.HOSPITAL as Hospital
import com.example.cordacashlessmediclaim.states.PartyRole.INSURER as Insurer
import com.example.cordacashlessmediclaim.states.PaymentReceiptState
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus as S
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException

class PaymentReceiptContract: Verifier<PaymentReceiptState>() {
    open class Commands: TypeOnlyCommandData() {
        class Create: Commands()
    }

    override val transitions = mapOf(
            Commands.Create() to TS(listOf(Insurer, Hospital), null, listOf(S.CREATED))
    )

    override fun verify(tx: LedgerTransaction) {
        val inputState = tx.inputStates.singleOrNull() as PaymentReceiptState?
        val outputState = tx.outputStates.singleOrNull() as PaymentReceiptState?
        val preAuthorizationState = tx.referenceStates.singleOrNull() as PreAuthorizationState?

        tx.commands.forEach {
            val ts = transitions[it.value] ?: throw
            IllegalArgumentException("Unable to get the value for key ${it.value} from transitions")
            verifyTransaction(ts, inputState, outputState, it.signers)

            when (it.value) {
                is Commands.Create -> {
                    requireThat {
                        "Currency in outputState must be the same as currency in preAuthorizationState" using
                                (outputState!!.currency == preAuthorizationState!!.currency)
                        "Amount in outputState must be equal to amount in preAuthorizationState" using
                                (outputState.amount.compareTo(preAuthorizationState.amount) == 0)
                        "Status in preAuthorizationState must be APPROVED" using
                                (preAuthorizationState.status == PreAuthorizationStatus.APPROVED)
                    }
                }
                else -> throw IllegalArgumentException("Unsupported command ${it.value}")
            }
        }
    }
}