package com.dcm.contract

import com.dcm.states.ModelState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class ModelContract: Contract {
    companion object{
        @JvmStatic
        val ID = "com.dcm.contract.ModelContract"
    }
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class UpdateCorpus : TypeOnlyCommandData(), Commands
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
                "The creator must be included in the list of signers." using (model.creator.owningKey in command.signers)
            }

            is Commands.UpdateCorpus -> requireThat {
                "One input state must be consumed when adding new data rows to a model." using (tx.inputs.size == 1)
                "One output state must be created when adding new data rows to a model." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<ModelState>().single()
                val output = tx.outputsOfType<ModelState>().single()
                "Proposed corpus cannot be the same as the inputState's." using (input.corpus != output.corpus)
                "You cannot change the model's labels." using (input.classificationReport.size == output.classificationReport.size)
                var delta = 0.00
                for((k,v) in input.classificationReport){
                    for ((x,y) in v){
                        delta += (output.classificationReport[k]?.get(x)!! - y)
                    }
                }
                "Your change must have some sort of positive affect on the model's classification report. Your delta was: $delta" using (delta > 0)
                "The creator must be included in the list of signers." using (output.creator.owningKey in command.signers)
            }
        }
    }
}