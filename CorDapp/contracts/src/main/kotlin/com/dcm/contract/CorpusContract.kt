package com.dcm.contract

import com.dcm.states.CorpusState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction


class CorpusContract: Contract {
    companion object{
        @JvmStatic
        val ID = "com.dcm.contract.CorpusContract"
    }
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class UpdateCorpus : TypeOnlyCommandData(), Commands
        class TransferOwnership : TypeOnlyCommandData(), Commands
        class UpdateClassifcationEndpoint : TypeOnlyCommandData(), Commands
        class SetClosedStatus : TypeOnlyCommandData(), Commands
        class CloseCorpus : TypeOnlyCommandData(), Commands
    }
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<CorpusContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing a Corpus." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a Corpus." using (tx.outputs.size == 1)
                val corpus = tx.outputsOfType<CorpusState>().single()
                "The participants of a corpus must have at least one party." using (corpus.participants.isNotEmpty())
                "The corpus cannot be empty." using (corpus.corpus.isNotEmpty())
                "The classification report cannot be empty." using(corpus.classificationReport.isNotEmpty())
                "The owner must be included in the list of signers." using (corpus.owner.owningKey in command.signers)
                "The proposed state status must be 'Open'" using (corpus.status == "Open")
            }

            is Commands.UpdateCorpus -> requireThat {
                "One input state must be consumed when adding new data rows to a corpus." using (tx.inputs.size == 1)
                "One output state must be created when adding new data rows to a corpus." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<CorpusState>().single()
                val output = tx.outputsOfType<CorpusState>().single()
                "Proposed corpus cannot be the same as the inputState's." using (input.corpus != output.corpus)
                "You cannot change the corpus's labels." using (input.classificationReport.size == output.classificationReport.size)
                var delta = 0.00
                for((k,v) in input.classificationReport){
                    for ((x,y) in v){
                        delta += (output.classificationReport[k]?.get(x)!! - y)
                    }
                }
                "Your change must have a positive affect on the corpus's classification report. Your delta was: $delta" using (delta > 0)
                "The classification URL cannot change during a corpus update." using (input.classificationURL  == output.classificationURL)
                "The classification update URL cannot change during a corpus update." using (input.classificationUpdateURL  == output.classificationUpdateURL)
                "The owner cannot change during a corpus update." using (input.owner.owningKey  == output.owner.owningKey)
                "The algorithm used cannot change during a corpus update." using (input.algorithmUsed  == output.algorithmUsed)
                "The owner must be included in the list of signers." using (output.owner.owningKey in command.signers)
                "The current status must be 'Open'" using (input.status == "Open")
                "The proposed state status must be 'Open'" using (output.status == "Open")
            }

            is Commands.TransferOwnership -> requireThat{
                "One input state must be consumed when transferring ownership of a corpus." using (tx.inputs.size == 1)
                "One output state must be created when transferring ownership of a corpus." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<CorpusState>().single()
                val output = tx.outputsOfType<CorpusState>().single()
                "You cannot change the corpus corpus while transferring ownership." using (input.corpus == output.corpus)
                "You cannot change the classification report while transferring ownership." using (input.classificationReport == output.classificationReport)
                "The current owner and the new owner cannot be the same." using (input.owner.owningKey != output.owner.owningKey)
                "The algorithm used cannot change during a corpus update." using (input.algorithmUsed == output.algorithmUsed)
                "The classification URL cannot change during a corpus update." using (input.classificationURL  == output.classificationURL)
                "The classification update URL cannot change during a corpus update." using (input.classificationUpdateURL  == output.classificationUpdateURL)
                "The previous owner must be included in the list of signers." using (input.owner.owningKey in command.signers)
                "The current status must be 'Open'" using (input.status == "Open")
                "The proposed state status must be 'Open'" using (output.status == "Open")
            }

            is Commands.UpdateClassifcationEndpoint -> requireThat {
                "One input state must be consumed when transferring ownership of a corpus." using (tx.inputs.size == 1)
                "One output state must be created when transferring ownership of a corpus." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<CorpusState>().single()
                val output = tx.outputsOfType<CorpusState>().single()
                "The algorithm used cannot change during an endpoint update." using (input.algorithmUsed == output.algorithmUsed)
                "You cannot change the corpus corpus during an endpoint update." using (input.corpus == output.corpus)
                "You cannot change the classification report while updating the classification endpoint." using (input.classificationReport == output.classificationReport)
                "You cannot change the owner of a corpus during an endpoint update" using (input.owner.owningKey == output.owner.owningKey)
                "The classification or update URL cannot be the same as the input state's." using ((input.classificationURL  != output.classificationURL) || (input.classificationUpdateURL  != output.classificationUpdateURL))
                "The previous owner must be included in the list of signers." using (input.owner.owningKey in command.signers)
                "The current status must be 'Open'" using (input.status == "Open")
                "The proposed state status must be 'Open'" using (output.status == "Open")
            }

            is Commands.SetClosedStatus -> {
                "One input state must be consumed when closing a corpus." using (tx.inputs.size == 1)
                "One output state must be created when closing a corpus." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<CorpusState>().single()
                val output = tx.outputsOfType<CorpusState>().single()
                "The owner must be included in the list of signers." using (input.owner.owningKey in command.signers)
                "The current status must be 'Open'" using (input.status == "Open")
                "The proposed status must be 'Closed'" using (output.status == "Closed")
                "The algorithm used cannot change during an endpoint update." using (input.algorithmUsed == output.algorithmUsed)
                "You cannot change the corpus corpus during an endpoint update." using (input.corpus == output.corpus)
                "You cannot change the classification report while updating the classification endpoint." using (input.classificationReport == output.classificationReport)
                "You cannot change the owner of a corpus during an endpoint update." using (input.owner.owningKey == output.owner.owningKey)
                "You cannot change the classificationURL." using (input.classificationURL == output.classificationURL)
                "You cannot change the classificationUpdateURL." using (input.classificationUpdateURL  == output.classificationUpdateURL)
            }

            is Commands.CloseCorpus -> requireThat {
                "One input state must be consumed when closing a corpus." using (tx.inputs.size == 1)
                "No output states must be created when closing a corpus." using (tx.outputs.isEmpty())
                val input = tx.inputsOfType<CorpusState>().single()
                "The owner must be included in the list of signers." using (input.owner.owningKey in command.signers)
                "The input state's status must be 'Closed'" using (input.status == "Closed")
            }
        }
    }
}