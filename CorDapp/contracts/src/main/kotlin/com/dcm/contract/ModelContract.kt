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
                "The corpus cannot be empty." using (model.corpus.isNotEmpty())
                "The classification report cannot be empty." using(model.classificationReport.isNotEmpty())
            }

            is Commands.AddDataRows -> requireThat {
                "Only one input state should be consumed when adding a new DataRow to a model." using (tx.inputs.size == 1)
                "Only one output state should be created when adding a new DataRow to a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "List of DataRows to add cannot be empty." using (input.corpus != output.corpus)
                "There must be more dataRows in the proposed model output state than the input state" using (input.corpus.size < output.corpus.size)
            }

            is Commands.RemoveDataRows -> requireThat {
                "Only one input state should be consumed when removing a DataRow from a model." using (tx.inputs.size == 1)
                "Only one output state should be created when removing a DataRow from a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "List of DataRows to add cannot be empty." using (input.corpus != output.corpus)
                "There must be less dataRows in the proposed model output state than the input state" using (input.corpus.size > output.corpus.size)
            }
        }
    }
}