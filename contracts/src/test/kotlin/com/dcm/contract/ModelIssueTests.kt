package com.dcm.contract

import com.dcm.states.ModelState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.Party
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import net.corda.training.CHARLIE
import org.junit.Test

class ModelIssueTests {
    private val ledgerServices = MockServices()
    class DummyCommand : TypeOnlyCommandData()

    /**
     * This Test checks the following while using the [ModelContract.Commands.Issue] command:
     * 1. There are no input states being consumed.
     * 2. Only one output state can be created.
     * 3. The gatekeepers of the [ModelState] has at least one Party in it.
     * 4. No other command type is allowed.
     */
    @Test
    fun mustIncludeIssueCommand() {
        val gatekeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
        val myCorpus = listOf<LinearState>()
        val dataRowMap = LinkedHashMap<String, DataRowState>()
        val model = ModelState(myCorpus, dataRowMap, gatekeepers)

        ledgerServices.ledger {
            // #1
            transaction {
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID,  model)
                command(gatekeepers.map{it.owningKey}, ModelContract.Commands.Issue())
                this.failsWith("No inputs should be consumed when issuing a Model")
            }

            // #2
            transaction {
                val gatekeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
                val myCorpus = listOf<LinearState>()
                val dataRowMap = LinkedHashMap<String, DataRowState>()
                val model2 = ModelState(myCorpus, dataRowMap, gatekeepers)
                output(ModelContract.MODEL_CONTRACT_ID,  model)
                output(ModelContract.MODEL_CONTRACT_ID, model2)
                command(gatekeepers.map{it.owningKey}, ModelContract.Commands.Issue())
                this.failsWith("Only one output state should be created when issuing a Model.")
            }

            // #3
            transaction {
                output(ModelContract.MODEL_CONTRACT_ID, ModelState(myCorpus, dataRowMap, listOf<Party>()))
                command(gatekeepers.map{it.owningKey}, ModelContract.Commands.Issue())
                this.failsWith("The participants of a model must have at least one party.")
            }

            // #4
            transaction {
                output(ModelContract.MODEL_CONTRACT_ID,  model)
                command(gatekeepers.map{it.owningKey}, DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                //output(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID, model)
                command(gatekeepers.map{it.owningKey}, ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }
}