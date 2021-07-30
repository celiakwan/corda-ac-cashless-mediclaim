package com.example.cordacashlessmediclaim.schemas

import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus
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

object PaymentReceiptSchema

object PaymentReceiptSchemaV1: MappedSchema(
        schemaFamily = PaymentReceiptSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPaymentReceipt::class.java)
) {
    override val migrationResource = "mediclaim.changelog-master"

    @Entity
    @Table(name = "payment_receipt_states")
    class PersistentPaymentReceipt(
            @Column(name = "payer_account_name")
            val payerAccountName: String,

            @Column(name = "payee_account_name")
            val payeeAccountName: String,

            @Column(name = "currency")
            val currency: Currency,

            @Column(name = "amount")
            val amount: BigDecimal,

            @Column(name = "submission_time")
            val submissionTime: Instant,

            @Column(name = "pre_authorization_linear_id")
            val preAuthorizationLinearId: UUID,

            @Enumerated(EnumType.STRING)
            @Column(name = "status")
            val status: PaymentReceiptStatus,

            @Column(name = "linear_id")
            var linearId: UUID
    ): PersistentState()
}