# corda-ac-cashless-mediclaim
A Corda project written in Kotlin to demonstrate the simplified processes of cashless medical insurance claims and creating payment receipts. It shows how to create accounts on the nodes, request new private/public key pairs for the accounts, exchange account public keys between the nodes, and how the states are stored in the vault.

[corda-bn-cashless-mediclaim](https://github.com/celiakwan/corda-bn-cashless-mediclaim) is another cashless mediclaim project implementing Corda business network instead.

### Version
- [Kotlin](https://kotlinlang.org/): 1.2.71
- [Corda](https://www.corda.net/): 4.8
- [Corda Accounts](https://github.com/corda/accounts): 1.0
- [Gradle](https://gradle.org/): 5.0.12

### Installation
Install Java 8 JDK.
```
brew tap adoptopenjdk/openjdk
brew install --cask adoptopenjdk8
```

### Configuration
`build.gradle` contains the configuration for 5 Corda nodes that grants the RPC user of each node permissions to run specific RPC operations and flows.
```
node {
    name "O=Notary,L=London,C=GB"
    notary = [validating : false]
    p2pPort 10002
    rpcSettings {
        address("localhost:10003")
        adminAddress("localhost:10043")
    }
}
node {
    name "O=Insurer,L=New York,C=US"
    p2pPort 10008
    rpcSettings {
        address("localhost:10009")
        adminAddress("localhost:10049")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.accountFlows.CreateAndShareAccountFlow",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.ApprovePreAuthorizationFlow",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.paymentReceiptFlows.CreatePaymentReceiptFlow"
                    ]
            ]
    ]
}
node {
    name "O=Hospital,L=New York,C=US"
    p2pPort 10011
    rpcSettings {
        address("localhost:10012")
        adminAddress("localhost:10052")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.accountFlows.CreateAndShareAccountFlow",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.CreatePreAuthorizationFlow"
                    ]
            ]
    ]
}
node {
    name "O=Patient,L=New York,C=US"
    p2pPort 10014
    rpcSettings {
        address("localhost:10015")
        adminAddress("localhost:10055")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows"
                    ]
            ]
    ]
}
```

### Build
Run the following Gradle task to build the nodes for testing. In the `build/nodes` directory, there will be files generated for the nodes such as CorDapp JARs, configuration files, certificates, etc.
```
./gradlew deployNodes
```

### Get Started
1. Open 4 terminal tabs and for each one set `JAVA_HOME` to the path where Java 8 JDK is located.
    ```
    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
    ```

2. Manually start each node in each terminal tab.
    ##### Notary
    ```
    cd build/nodes/Notary
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Insurer
    ```
    cd build/nodes/Insurer
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Hospital
    ```
    cd build/nodes/Hospital
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Patient
    ```
    cd build/nodes/Patient
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

### Testing
##### Insurer
1. Create 2 accounts `INSURER_CLAIMS_OFFICER` and `INSURER_FINANCE_CLERK`, and share their account info to Hospital.
    ```
    flow start CreateAndShareAccountFlow newAccounts: [ { first: INSURER_CLAIMS_OFFICER, second: ["O=Hospital,L=New York,C=US"] } ]
    flow start CreateAndShareAccountFlow newAccounts: [ { first: INSURER_FINANCE_CLERK, second: ["O=Hospital,L=New York,C=US"] } ]
    ```

##### Hospital
2. Create 2 accounts `HOSPITAL_REGISTRAR` and `HOSPITAL_FINANCE_CLERK`, and share their account info to Insurer.
    ```
    flow start CreateAndShareAccountFlow newAccounts: [ { first: HOSPITAL_REGISTRAR, second: ["O=Insurer,L=New York,C=US"] } ]
    flow start CreateAndShareAccountFlow newAccounts: [ { first: HOSPITAL_FINANCE_CLERK, second: ["O=Insurer,L=New York,C=US"] } ]
    ```

3. Query the `AccountInfo` from the vault.
    ```
    run vaultQuery contractStateType: com.r3.corda.lib.accounts.contracts.states.AccountInfo
    ```

4. Submit a pre-authorization.
    ```
    flow start CreatePreAuthorizationFlow policyHolder: "O=Patient,L=New York,C=US", membershipNumber: 00001, provider: HOSPITAL_REGISTRAR, diagnosisDescription: Stroke, currency: USD, amount: 300, policyIssuer: INSURER_CLAIMS_OFFICER
    ```

5. Query the `PreAuthorizationState` from the vault and get the `linearId.id`.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PreAuthorizationState
    ```

##### Insurer
6. Query the `AccountInfo` from the vault.
    ```
    run vaultQuery contractStateType: com.r3.corda.lib.accounts.contracts.states.AccountInfo
    ```

7. Approve the pre-authorization.
    ```
    flow start ApprovePreAuthorizationFlow linearId: 7874b622-e341-4282-941f-5d2835544fb2
    ```

##### Patient
8. Query the `PreAuthorizationState` from the vault.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PreAuthorizationState
    ```

##### Insurer
9. Send a payment receipt.
    ```
    flow start CreatePaymentReceiptFlow payer: INSURER_FINANCE_CLERK, payee: HOSPITAL_FINANCE_CLERK, currency: USD, amount: 300, preAuthorizationLinearId: 7874b622-e341-4282-941f-5d2835544fb2
    ```

##### Hospital
10. Query the `PaymentReceiptState` from the vault.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PaymentReceiptState
    ```