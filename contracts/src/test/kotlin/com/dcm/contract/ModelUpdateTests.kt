package com.dcm.contract

import com.dcm.states.ModelState
import com.dcm.states.DataRowState
import net.corda.core.contracts.LinearState
import net.corda.core.identity.Party
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import net.corda.training.CHARLIE
import net.corda.training.MINICORP
import org.junit.Test
import java.security.PublicKey

class ModelUpdateTests {
    private val ledgerServices = MockServices()
//    class DummyCommand : TypeOnlyCommandData()
//    class DummyState : ContractState {
//        override val participants: List<AbstractParty> get() = listOf()
//    }

    /**
     * 1. This test is focused on the functionality of the [ModelState.addModelGatekeepers] command.
     *    - Only one input and output states.
     *    - The inputState.aatekeepers.size() > outputState.gatekeepers.size()
     *    - The signers should be the same parties in the gatekeeper list.
     * 2. [ModelState.removeModelGatekeepers]
     * 3. [ModelState.addCorpusDataRows]
     * 4. [ModelState.removeCorpusDataRows]
     */
    @Test
    fun testAddModelGateKeepers(){

        ledgerServices.ledger {
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party, BOB.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID,  model.addGateKeepers(BOB.party))
                command(gatekeepers.map { it.owningKey }, ModelContract.Commands.AddGateKeepers())
                this.failsWith("The inputState's gatekeeper list should have grown larger than that of the outputState's.")
            }
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID,  model.addGateKeepers(BOB.party))
                command(model.participants.map { it.owningKey }, ModelContract.Commands.AddGateKeepers())
                this.failsWith("The list of parties for required signers should be equivalent to the output state's proposed gatekeeper list.")
            }
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID,  model.addGateKeepers(BOB.party))
                command(model.participants.map{it.owningKey} + BOB.publicKey, ModelContract.Commands.AddGateKeepers())
                this.verifies()
            }
        }
    }
    /**
     * 2. This test is focused on the functionality of the [ModelState.removeModelGatekeepers] command.
     *    - Only one input and output states.
     *    - The inputState.aatekeepers.size() < outputState.gatekeepers.size()
     *    - The signers should be the same parties as the inputeState's participant list.
     *    - Make sure that there is always at least one gatekeeper in the participant list.
    */
    @Test
    fun testRemoveModelGateKeepers() {
        ledgerServices.ledger {
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID, model)
                command(model.participants.map { it.owningKey }, ModelContract.Commands.RemoveGateKeepers())
                this.failsWith("The inputState's gatekeeper list should have shrunk smaller than that of the outputState's.")
            }
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party, BOB.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID, model.removeGateKeepers(BOB.party))
                command(model.participants.minus(BOB.party).map { it.owningKey }, ModelContract.Commands.RemoveGateKeepers())
                this.failsWith("The list of parties for required signers should be equivalent to the input state's proposed gatekeeper list.")
            }
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID, model.removeGateKeepers(ALICE.party))
                command(gatekeepers.map { it.owningKey }, ModelContract.Commands.RemoveGateKeepers())
                this.failsWith("There must always be at least one gatekeeper in the gatekeeper list at all times.")
            }
            transaction {
                val myCorpus = listOf<LinearState>()
                val gatekeepers = listOf<Party>(ALICE.party, BOB.party)
                val model = ModelState(myCorpus, gatekeepers)
                input(ModelContract.MODEL_CONTRACT_ID, model)
                output(ModelContract.MODEL_CONTRACT_ID, model.removeGateKeepers(BOB.party))
                command(model.participants.map { it.owningKey }, ModelContract.Commands.RemoveGateKeepers())
                this.verifies()
            }
        }
    }
//    @Test
//    fun testAddRemoveCombined() {
//        ledgerServices.ledger {
//            val myCorpus = listOf<DataRowState>()
//            val gateKeepers = listOf<Party>(ALICE.party, BOB.party, CHARLIE.party)
//            val model = ModelState(myCorpus, gateKeepers)
//            transaction {
//                output(ModelContract.MODEL_CONTRACT_ID, model)
//                command(gateKeepers.map { it.owningKey }, ModelContract.Commands.Issue())
//
//                input(ModelContract.MODEL_CONTRACT_ID, model)
//                val removeAliceState = model.removeGateKeepers(ALICE.party)
//                output(ModelContract.MODEL_CONTRACT_ID, removeAliceState)
//                command(gateKeepers.map { it.owningKey }, ModelContract.Commands.RemoveGateKeepers())
//
//                input(ModelContract.MODEL_CONTRACT_ID, removeAliceState)
//                output(ModelContract.MODEL_CONTRACT_ID, removeAliceState.addGateKeepers(MINICORP.party))
//                command(removeAliceState.participants.map { it.owningKey }.plus(MINICORP.publicKey), ModelContract.Commands.AddGateKeepers())
//                this.verifies()
//            }
//        }
//    }
//    @Test
//    fun testAddDataRows(){
//
//    }

//    @Test
//    fun testRemoveDataRows(){
//
//    }
}