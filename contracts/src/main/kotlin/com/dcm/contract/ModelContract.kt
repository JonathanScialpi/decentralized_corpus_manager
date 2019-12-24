package com.dcm.contract

import com.dcm.states.ModelState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class ModelContract: Contract {
    companion object{
        @JvmStatic
        val MODEL_CONTRACT_ID = "com.dcm.contract.ModelContract"
    }
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class AddDataRows : TypeOnlyCommandData(), Commands
        class RemoveDataRows : TypeOnlyCommandData(), Commands
        class AddGateKeepers : TypeOnlyCommandData(), Commands
        class RemoveGateKeepers : TypeOnlyCommandData(), Commands
    }
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<ModelContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing a Model." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a Model." using (tx.outputs.size == 1)
                val model = tx.outputsOfType<ModelState>().single()
                "The participants of a model must have at least one party." using (model.participants.isNotEmpty())
            }

            is Commands.AddGateKeepers -> requireThat {
                "Only one input state should be consumed when adding a new gatekeeper to a model." using (tx.inputs.size == 1)
                "Only one output state should be created when adding a new gatekeeper to a model" using (tx.outputs.size == 1)
                val inputModel = tx.inputsOfType<ModelState>().single()
                val outputModel = tx.outputsOfType<ModelState>().single()
                "The inputState's gatekeeper list should have grown larger than that of the outputState's." using (inputModel.participants.size < outputModel.participants.size)
                "The list of parties for required signers should be equivalent to the output state's proposed gatekeeper list." using (command.signers == outputModel.participants.map { it.owningKey })
            }

            is Commands.RemoveGateKeepers -> requireThat {
                "Only one input state should be consumed when removing a new gatekeeper to a model." using (tx.inputs.size == 1)
                "Only one output state should be created when removing a new gatekeeper to a model." using (tx.outputs.size == 1)
                val outputModel = tx.outputsOfType<ModelState>().single()
                "There must always be at least one gatekeeper in the gatekeeper list at all times." using (outputModel.participants.isNotEmpty())
                val inputModel = tx.inputsOfType<ModelState>().single()
                "The list of parties for required signers should be equivalent to the input state's proposed gatekeeper list." using (command.signers == inputModel.participants.map { it.owningKey })
                "The inputState's gatekeeper list should have shrunk smaller than that of the outputState's." using (inputModel.participants.size > outputModel.participants.size)
            }

            is Commands.AddDataRows -> requireThat {
                "Only one input state should be consumed when adding a new DataRow to a model." using (tx.inputs.size == 1)
                "Only one output state should be created when adding a new DataRow to a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "List of DataRows to add cannot be empty." using (input.dataRowMap != output.dataRowMap)
            }

            is Commands.RemoveDataRows -> requireThat {
                "Only one input state should be consumed when removing a DataRow from a model." using (tx.inputs.size == 1)
                "Only one output state should be created when removing a DataRow from a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "List of DataRows to add cannot be empty." using (input.dataRowMap != output.dataRowMap)
            }
        }
    }
}