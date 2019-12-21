package com.dcm.contract

import com.dcm.states.DataRowState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class DataRowContract : Contract {
    companion object{
        @JvmStatic
        val DATAROW_CONTRACT_ID = "com.dcm.contract.DataRowContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class UpdateDataRow : TypeOnlyCommandData(), Commands
        class ChangeParentModel : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<DataRowContract.Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing a Model." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a Model." using (tx.outputs.size == 1)
                val output = tx.outputsOfType<DataRowState>().single()
                "The DataRow must pointt to its respective ModelState." using (output.parentModel == null)
            }
            is Commands.UpdateDataRow -> requireThat {
                val input = tx.inputsOfType<DataRowState>().single()
                val output = tx.outputsOfType<DataRowState>().single()
                "The proposed update is the same as the original value." using (input.dataRow == output.dataRow)
            }
            is Commands.ChangeParentModel -> requireThat {
                val input = tx.inputsOfType<DataRowState>().single()
                val output = tx.outputsOfType<DataRowState>().single()
                "The proposed parent model state is the same as the original." using (input.parentModel == output.parentModel)
            }
        }
    }
}