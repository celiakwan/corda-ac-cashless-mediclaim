package com.example.cordacashlessmediclaim.contracts.verification

import com.example.cordacashlessmediclaim.states.VerifiableState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import java.security.PublicKey
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

abstract class Verifier<T: VerifiableState<*>>: Contract {
    abstract val transitions: Map<out TypeOnlyCommandData, Transition>

    fun verifyTransaction(
            transition: Transition,
            inputState: T?,
            outputState: T?,
            signers: List<PublicKey>
    ) {
        lateinit var expectedSigners: List<PublicKey>
        lateinit var linearId: UniqueIdentifier
        if (transition.expectedInputStatus == null) {
            expectedSigners = transition.expectedSigners.map { outputState!!.partyRoleToParty(it).owningKey }
            linearId = outputState!!.linearId
        }
        else {
            expectedSigners = transition.expectedSigners.map { inputState!!.partyRoleToParty(it).owningKey }
            linearId = inputState!!.linearId
        }

        // Verify signers.
        requireThat {
            "Illegal transaction signers for the state with linear ID $linearId" using
                    (signers.size == expectedSigners.size && signers.containsAll(expectedSigners))
        }

        // Verify status.
        requireThat {
            "Invalid inputState status. Expected: ${transition.expectedInputStatus}, actual: ${inputState?.status}" using
                    (inputState?.status == transition.expectedInputStatus)
            "Invalid outputState status. Expected: ${transition.expectedOutputStatuses}, actual: ${outputState?.status}" using
                    (if (transition.expectedOutputStatuses.isEmpty()) outputState == null
                    else outputState!!.status in transition.expectedOutputStatuses)
        }
    }

    inline fun <reified R: T> isUnchangedExcept(inputState: R, outputState: R, except: KProperty1<R, *>) {
        requireThat {
            for (prop in R::class.memberProperties) {
                if (prop == except) {
                    continue
                }
                "Property ${prop.name} must be unchanged" using (prop.get(inputState) == prop.get(outputState))
            }
        }
    }
}