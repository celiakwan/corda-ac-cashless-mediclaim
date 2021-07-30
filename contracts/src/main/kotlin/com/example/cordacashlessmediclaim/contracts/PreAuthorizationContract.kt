package com.example.cordacashlessmediclaim.contracts

import com.example.cordacashlessmediclaim.contracts.verification.Transition as TS
import com.example.cordacashlessmediclaim.contracts.verification.Verifier
import com.example.cordacashlessmediclaim.states.PartyRole.HOSPITAL as Hospital
import com.example.cordacashlessmediclaim.states.PartyRole.INSURER as Insurer
import com.example.cordacashlessmediclaim.states.PartyRole.PATIENT as Patient
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus as S
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.math.BigDecimal

class PreAuthorizationContract: Verifier<PreAuthorizationState>() {
    open class Commands: TypeOnlyCommandData() {
        class Create: Commands()
        class Approve: Commands()
    }

    override val transitions = mapOf(
            Commands.Create() to TS(listOf(Hospital, Patient), null, listOf(S.CREATED)),
            Commands.Approve() to TS(listOf(Insurer), S.CREATED, listOf(S.APPROVED))
    )

    override fun verify(tx: LedgerTransaction) {
        val inputState = tx.inputStates.singleOrNull() as PreAuthorizationState?
        val outputState = tx.outputStates.singleOrNull() as PreAuthorizationState?

        tx.commands.forEach {
            val ts = transitions[it.value] ?: throw
            IllegalArgumentException("Unable to get the value for key ${it.value} from transitions")
            verifyTransaction(ts, inputState, outputState, it.signers)

            when (it.value) {
                is Commands.Create -> {
                    requireThat {
                        "Amount must be greater than zero" using (outputState!!.amount.compareTo(BigDecimal(0)) == 1)
                    }
                }
                is Commands.Approve -> {
                    isUnchangedExcept(inputState!!, outputState!!, PreAuthorizationState::status)
                }
                else -> throw IllegalArgumentException("Unsupported command ${it.value}")
            }
        }
    }
}