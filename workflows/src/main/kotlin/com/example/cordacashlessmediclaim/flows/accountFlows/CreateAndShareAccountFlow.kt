package com.example.cordacashlessmediclaim.flows.accountFlows

import co.paralleluniverse.fibers.Suspendable
import com.example.cordacashlessmediclaim.data.AccountRole
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party

@InitiatingFlow
@StartableByRPC
class CreateAndShareAccountFlow(private val newAccounts: List<Pair<AccountRole, List<Party>>>): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        newAccounts.forEach {
            val accountName = it.first.name
            if (accountService.accountInfo(accountName).isNotEmpty()) {
                throw FlowException("Account name $accountName already exists")
            }
            val accountInfoStateAndRef = subFlow(CreateAccount(accountName))
            subFlow(ShareAccountInfo(accountInfoStateAndRef, it.second))
        }
    }
}