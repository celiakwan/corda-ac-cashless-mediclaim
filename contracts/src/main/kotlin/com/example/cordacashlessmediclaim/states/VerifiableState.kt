package com.example.cordacashlessmediclaim.states

import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

interface VerifiableState<E: Enum<E>>: LinearState {
    val status: Enum<E>
    fun partyRoleToParty(partyRole: PartyRole): AbstractParty
}

@CordaSerializable
enum class PartyRole {
    PATIENT, HOSPITAL, INSURER
}