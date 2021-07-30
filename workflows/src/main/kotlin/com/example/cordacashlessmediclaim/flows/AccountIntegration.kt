package com.example.cordacashlessmediclaim.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

abstract class AccountIntegration<T>: FlowLogic<T>() {
    protected fun getAccountInfo(accountName: String): AccountInfo {
        return accountService.accountInfo(accountName).singleOrNull()?.state?.data
                ?: throw FlowException("Account with name $accountName not found")
    }

    protected fun newKeyFor(accountInfo: AccountInfo): PublicKey =
            serviceHub.keyManagementService.freshKey(accountInfo.identifier.id)

    @Suspendable
    protected fun requestKeyFor(accountInfo: AccountInfo): AnonymousParty =
            subFlow(RequestKeyForAccount(accountInfo))
}