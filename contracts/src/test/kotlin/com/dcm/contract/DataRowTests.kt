package com.dcm.contract

import com.dcm.states.DataRowState
import com.dcm.states.ModelState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.Party
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import net.corda.training.CHARLIE
import org.apache.activemq.artemis.utils.DataConstants
import org.junit.Test

class DataRowTests {
    private val ledgerServices = MockServices()
    class DummyCommand : TypeOnlyCommandData()

    /**
     * This Test checks the following while using the [DataRowContract.Commands.Issue] command:
     * 1. There are no input states being consumed.
     * 2. Only one output state can be created.
     * 3. The DataRow must have a ModelID associated with it.
     * 4. No other command type is allowed.
     */
    @Test
    fun dataRowIssueTesting() {
        val gatekeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
        val myCorpus = listOf<LinearState>()
        val model = ModelState(myCorpus, gatekeepers)
        val dr = DataRowState(
                dataRow = "Help me change my password!",
                parentModel = model
        )
        ledgerServices.ledger {
            transaction {
                val dr2 = DataRowState(
                        dataRow = "Don't help me change the password?",
                        parentModel = model
                )
                input(DataRowContract.DATAROW_CONTRACT_ID, dr2)
                output(DataRowContract.DATAROW_CONTRACT_ID, dr)
                command(model.participants.map{it.owningKey}, DataRowContract.Commands.Issue())
                this.failsWith("No inputs should be consumed when issuing a DataRow.")
            }
            transaction {
                output(DataRowContract.DATAROW_CONTRACT_ID, dr)
                command(model.participants.map{it.owningKey}, DataRowContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun testingUpdateDataRow(){
        val gatekeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
        val myCorpus = listOf<LinearState>()
        val model = ModelState(myCorpus, gatekeepers)
        val dr = DataRowState(
                dataRow = "Help me change my password!",
                parentModel = model
        )
      ledgerServices.ledger {
          transaction {
              val dr2 = dr.updateDataRow("Help me change my password!")
              input(DataRowContract.DATAROW_CONTRACT_ID, dr)
              output(DataRowContract.DATAROW_CONTRACT_ID, dr2)
              command(model.participants.map { it.owningKey }, DataRowContract.Commands.UpdateDataRow())
              this.failsWith("The proposed update is the same as the original value.")
          }
          transaction {
              val dr2 = dr.updateDataRow("Changed the dataRow value.")
              input(DataRowContract.DATAROW_CONTRACT_ID, dr)
              output(DataRowContract.DATAROW_CONTRACT_ID, dr2)
              command(model.participants.map { it.owningKey }, DataRowContract.Commands.UpdateDataRow())
              this.verifies()
          }
      }
    }
    @Test
    fun testingChangeParentModel(){
        val gatekeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
        val myCorpus = listOf<LinearState>()
        val model = ModelState(myCorpus, gatekeepers)
        val dr = DataRowState(
                dataRow = "Help me change my password!",
                parentModel = model
        )
        ledgerServices.ledger {
            transaction {
                val dr2 = dr.changeParentModel(model)
                input(DataRowContract.DATAROW_CONTRACT_ID, dr)
                output(DataRowContract.DATAROW_CONTRACT_ID, dr2)
                command(model.participants.map { it.owningKey }, DataRowContract.Commands.ChangeParentModel())
                this.failsWith("The proposed parent model state is the same as the original.")
            }
            transaction {
                val dr2 = dr.changeParentModel(ModelState(myCorpus, gatekeepers.minus(BOB.party)))
                input(DataRowContract.DATAROW_CONTRACT_ID, dr)
                output(DataRowContract.DATAROW_CONTRACT_ID, dr2)
                command(model.participants.map { it.owningKey }, DataRowContract.Commands.ChangeParentModel())
                this.verifies()
            }
        }
    }
}