package com.example.cordacashlessmediclaim.data

import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Account(val party: AnonymousParty, val role: AccountRole)