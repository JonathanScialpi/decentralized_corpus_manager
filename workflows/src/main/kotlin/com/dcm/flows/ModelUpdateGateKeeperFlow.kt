package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ModelUpdateGateKeeperFlow(private val inputStateLinearId: UniqueIdentifier,
                                private val outputState: ModelState,
                                private val addingGateKeeper: Boolean
                                ): FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(inputStateLinearId))
        val modelInputStateRef =  serviceHub.vaultService.queryBy<ModelState>(queryCriteria).states.single()

        //make sure that the party running this flow is already a gatekeeper
        if(ourIdentity !in modelInputStateRef.state.data.participants){
            throw IllegalArgumentException("The initiator of this flow must be a gatekeeper.")
        }
        val outputStateAndContract = StateAndContract(outputState, ModelContract.MODEL_CONTRACT_ID)
        var gatekeeperCommand = if(addingGateKeeper){
            Command(ModelContract.Commands.AddGateKeepers(), outputState.participants.map { it.owningKey })
        }else{
            Command(ModelContract.Commands.RemoveGateKeepers(), outputState.participants.map { it.owningKey })
        }
        val txBuilder = TransactionBuilder(notary = notary).withItems(
                modelInputStateRef,
                outputStateAndContract,
                gatekeeperCommand
        )
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = (outputState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(ModelUpdateGateKeeperFlow::class)
class ModelAddGateKeeperResponderFlow(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Model State transaction" using (output is ModelState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}