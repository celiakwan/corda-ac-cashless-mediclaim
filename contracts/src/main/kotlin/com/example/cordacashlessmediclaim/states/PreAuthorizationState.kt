package com.example.cordacashlessmediclaim.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import com.example.cordacashlessmediclaim.contracts.PreAuthorizationContract
import com.example.cordacashlessmediclaim.data.Account
import com.example.cordacashlessmediclaim.schemas.PreAuthorizationSchemaV1
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

@BelongsToContract(PreAuthorizationContract::class)
data class PreAuthorizationState(
        val policyHolder: Party,
        val membershipNumber: String,
        val providerAccount: Account,
        val diagnosisDescription: String,
        val currency: Currency,
        val amount: BigDecimal,
        val policyIssuerAccount: Account,
        val submissionTime: Instant,
        override val status: PreAuthorizationStatus,
        override val participants: List<AbstractParty> = listOf(
                policyHolder,
                providerAccount.party,
                policyIssuerAccount.party
        ),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
): VerifiableState<PreAuthorizationStatus>, QueryableState {
    override fun partyRoleToParty(partyRole: PartyRole): AbstractParty =
            when (partyRole) {
                PartyRole.PATIENT -> policyHolder
                PartyRole.HOSPITAL -> providerAccount.party
                PartyRole.INSURER -> policyIssuerAccount.party
            }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PreAuthorizationSchemaV1 -> PreAuthorizationSchemaV1.PersistentPreAuthorization(
                    policyHolderName = policyHolder.name.toString(),
                    membershipNumber = membershipNumber,
                    providerAccountName = providerAccount.role.name,
                    diagnosisDescription = diagnosisDescription,
                    currency = currency,
                    amount = amount,
                    policyIssuerAccountName = policyIssuerAccount.role.name,
                    submissionTime = submissionTime,
                    status = status,
                    linearId = linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PreAuthorizationSchemaV1)
}

@CordaSerializable
enum class PreAuthorizationStatus {
    CREATED, APPROVED
}