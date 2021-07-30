package com.example.cordacashlessmediclaim.data

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class AccountRole {
    INSURER_CLAIMS_OFFICER,
    INSURER_FINANCE_CLERK,
    HOSPITAL_REGISTRAR,
    HOSPITAL_FINANCE_CLERK
}