package com.example.cordacashlessmediclaim.contracts.verification

import com.example.cordacashlessmediclaim.states.PartyRole
import net.corda.core.contracts.requireThat

data class Transition(
        val expectedSigners: List<PartyRole>,
        val expectedInputStatus: Enum<*>?,
        val expectedOutputStatuses: List<Enum<*>>
) {
    init {
        requireThat {
            "Nullable expectedInputStatus and empty expectedOutputStatus existing together is not allowed" using
                    (!(expectedInputStatus == null && expectedOutputStatuses.isEmpty()))
        }
    }
}