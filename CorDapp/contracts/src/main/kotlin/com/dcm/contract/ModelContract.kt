package com.dcm.contract

import com.dcm.states.ModelState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class ModelContract: Contract {
    companion object{
        @JvmStatic
        val ID = "com.dcm.contract.ModelContract"
    }
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class UpdateCorpus : TypeOnlyCommandData(), Commands
//        class RemoveDataRows : TypeOnlyCommandData(), Commands
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

            is Commands.UpdateCorpus -> requireThat {
                "Only one input state should be consumed when adding a new DataRow to a model." using (tx.inputs.size == 1)
                "Only one output state should be created when adding a new DataRow to a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "Proposed corpus cannot be the same as the inputState's." using (input.corpus != output.corpus)
                //"There must be more dataRows in the proposed model output state than the input state" using (input.corpus.size < output.corpus.size)
                "You cannot change the model's labels." using (input.classificationReport.size == output.classificationReport.size)
                var delta = 0.00
                for((k,v) in input.classificationReport){
                    for ((x,y) in v){
                        delta += (output.classificationReport[k]?.get(x)!! - y)
                    }
                }
                "Your change must have some sort of positive affect on the model's classification report" using (delta > 0)
            }
        }
    }
}