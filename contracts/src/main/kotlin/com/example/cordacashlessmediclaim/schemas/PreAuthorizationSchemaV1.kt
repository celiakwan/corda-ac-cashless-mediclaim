package com.example.cordacashlessmediclaim.schemas

import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

object PreAuthorizationSchema

object PreAuthorizationSchemaV1: MappedSchema(
        schemaFamily = PreAuthorizationSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPreAuthorization::class.java)
) {
    override val migrationResource = "mediclaim.changelog-master"

    @Entity
    @Table(name = "pre_authorization_states")
    class PersistentPreAuthorization(
            @Column(name = "policy_holder_name")
            val policyHolderName: String,

            @Column(name = "membership_number")
            val membershipNumber: String,

            @Column(name = "provider_account_name")
            val providerAccountName: String,

            @Column(name = "diagnosis_description")
            val diagnosisDescription: String,

            @Column(name = "currency")
            val currency: Currency,

            @Column(name = "amount")
            val amount: BigDecimal,

            @Column(name = "policy_issuer_account_name")
            val policyIssuerAccountName: String,

            @Column(name = "submission_time")
            val submissionTime: Instant,

            @Enumerated(EnumType.STRING)
            @Column(name = "status")
            val status: PreAuthorizationStatus,

            @Column(name = "linear_id")
            var linearId: UUID
    ): PersistentState()
}