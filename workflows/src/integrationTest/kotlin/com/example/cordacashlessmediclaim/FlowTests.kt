package com.example.cordacashlessmediclaim

import com.example.cordacashlessmediclaim.flows.accountFlows.CreateAndShareAccountFlow
import com.example.cordacashlessmediclaim.flows.paymentReceiptFlows.CreatePaymentReceiptFlow
import com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.ApprovePreAuthorizationFlow
import com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.CreatePreAuthorizationFlow
import com.example.cordacashlessmediclaim.data.AccountRole
import com.example.cordacashlessmediclaim.states.PaymentReceiptState
import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var insurerNode: StartedMockNode
    private lateinit var hospitalNode: StartedMockNode
    private lateinit var patientNode: StartedMockNode
    private lateinit var insurer: Party
    private lateinit var hospital: Party
    private lateinit var patient: Party

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.example.cordacashlessmediclaim.contracts"),
                        TestCordapp.findCordapp("com.example.cordacashlessmediclaim.flows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.ci.workflows")
                ),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
        ))
        insurerNode = network.createPartyNode(CordaX500Name(organisation = "Insurer", locality = "New York", country = "US"))
        hospitalNode = network.createPartyNode(CordaX500Name(organisation = "Hospital", locality = "New York", country = "US"))
        patientNode = network.createPartyNode(CordaX500Name(organisation = "Patient", locality = "New York", country = "US"))
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun integrationTest() {
        insurer = insurerNode.info.legalIdentities.single()
        hospital = hospitalNode.info.legalIdentities.single()
        patient = patientNode.info.legalIdentities.single()

        val preAuthorizationLinearId = preAuthorizationTest()
        paymentReceiptTest(preAuthorizationLinearId)
    }
    
    private fun preAuthorizationTest(): UniqueIdentifier {
        // Create accounts and share the account info to the specific parties.
        insurerNode.startFlow(CreateAndShareAccountFlow(listOf(Pair(
                AccountRole.INSURER_CLAIMS_OFFICER, listOf(hospital)
        ))))
        insurerNode.startFlow(CreateAndShareAccountFlow(listOf(Pair(
                AccountRole.INSURER_FINANCE_CLERK, listOf(hospital)
        ))))
        hospitalNode.startFlow(CreateAndShareAccountFlow(listOf(Pair(
                AccountRole.HOSPITAL_REGISTRAR, listOf(insurer)
        ))))
        hospitalNode.startFlow(CreateAndShareAccountFlow(listOf(Pair(
                AccountRole.HOSPITAL_FINANCE_CLERK, listOf(insurer)
        ))))
        network.runNetwork()

        val insurerVisibleAccounts = insurerNode.services.vaultService.queryBy<AccountInfo>().states
        val insurerAccounts = insurerNode.services.accountService.ourAccounts()
        val hospitalVisibleAccounts = hospitalNode.services.vaultService.queryBy<AccountInfo>().states
        val hospitalAccounts = hospitalNode.services.accountService.ourAccounts()
        assertEquals(4, insurerVisibleAccounts.size)
        assertEquals(2, insurerAccounts.size)
        assertEquals(4, hospitalVisibleAccounts.size)
        assertEquals(2, hospitalAccounts.size)

        // Create a pre-authorization.
        hospitalNode.startFlow(CreatePreAuthorizationFlow(
                patient,
                "00001",
                AccountRole.HOSPITAL_REGISTRAR,
                "Stroke",
                Currency.getInstance("USD"),
                BigDecimal(300),
                AccountRole.INSURER_CLAIMS_OFFICER
        ))
        network.runNetwork()

        val insurerClaimsOfficer = insurerNode.services.accountService
                .accountInfo(AccountRole.INSURER_CLAIMS_OFFICER.name).single().state.data
        val hospitalRegistrar = hospitalNode.services.accountService
                .accountInfo(AccountRole.HOSPITAL_REGISTRAR.name).single().state.data
        var insurerPreAuthorizationState = insurerNode.services.vaultService.queryBy<PreAuthorizationState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(insurerClaimsOfficer.identifier.id))
        ).states.single().state.data
        var hospitalPreAuthorizationState = hospitalNode.services.vaultService.queryBy<PreAuthorizationState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(hospitalRegistrar.identifier.id))
        ).states.single().state.data
        var patientPreAuthorizationState = patientNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        assertEquals(PreAuthorizationStatus.CREATED, insurerPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.CREATED, hospitalPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.CREATED, patientPreAuthorizationState.status)

        // Approve the pre-authorization.
        insurerNode.startFlow(ApprovePreAuthorizationFlow(insurerPreAuthorizationState.linearId))
        network.runNetwork()

        insurerPreAuthorizationState = insurerNode.services.vaultService.queryBy<PreAuthorizationState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(insurerClaimsOfficer.identifier.id))
        ).states.single().state.data
        hospitalPreAuthorizationState = hospitalNode.services.vaultService.queryBy<PreAuthorizationState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(hospitalRegistrar.identifier.id))
        ).states.single().state.data
        patientPreAuthorizationState = patientNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        assertEquals(PreAuthorizationStatus.APPROVED, insurerPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.APPROVED, hospitalPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.APPROVED, patientPreAuthorizationState.status)

        return insurerPreAuthorizationState.linearId
    }

    private fun paymentReceiptTest(preAuthorizationLinearId: UniqueIdentifier) {
        // Create a payment receipt.
        insurerNode.startFlow(CreatePaymentReceiptFlow(
                AccountRole.INSURER_FINANCE_CLERK,
                AccountRole.HOSPITAL_FINANCE_CLERK,
                Currency.getInstance("USD"),
                BigDecimal(300),
                preAuthorizationLinearId
        ))
        network.runNetwork()

        val insurerFinanceClerk = insurerNode.services.accountService
                .accountInfo(AccountRole.INSURER_FINANCE_CLERK.name).single().state.data
        val hospitalFinanceClerk = hospitalNode.services.accountService
                .accountInfo(AccountRole.HOSPITAL_FINANCE_CLERK.name).single().state.data
        val insurerPaymentReceiptState = insurerNode.services.vaultService.queryBy<PaymentReceiptState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(insurerFinanceClerk.identifier.id))
        ).states.single().state.data
        val hospitalPaymentReceiptState = hospitalNode.services.vaultService.queryBy<PaymentReceiptState>(
                QueryCriteria.VaultQueryCriteria(externalIds = listOf(hospitalFinanceClerk.identifier.id))
        ).states.single().state.data

        assertEquals(PaymentReceiptStatus.CREATED, insurerPaymentReceiptState.status)
        assertEquals(PaymentReceiptStatus.CREATED, hospitalPaymentReceiptState.status)
    }
}