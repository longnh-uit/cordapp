package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class HouseTransferFlowInitiator extends FlowLogic<SignedTransaction> {
    private final String address;
    private final Party newOwner;

    public HouseTransferFlowInitiator(String address, Party newOwner) {
        this.address = address;
        this.newOwner = newOwner;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        List<StateAndRef<HouseState>> houseStateAndRefs = getServiceHub().getVaultService().queryBy(HouseState.class).getStates();

        StateAndRef<HouseState> inputHouseStateAndRef = houseStateAndRefs
                .stream().filter(houseStateAndRef -> {
                    HouseState houseState = houseStateAndRef.getState().getData();
                    return houseState.getAddress().equals(address);
                }).findAny().orElseThrow(() -> new IllegalArgumentException("The house was not found."));

        HouseState inputHouseState = inputHouseStateAndRef.getState().getData();

        // check if the flow was not start by the art's current owner.
        if (!getOurIdentity().equals(inputHouseState.getOwner()))
            throw new IllegalStateException("This flow must be started by the current owner.");

        Party notary = inputHouseStateAndRef.getState().getNotary();

        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        txBuilder.addInputState(inputHouseStateAndRef);

        HouseState outputHouseState = new HouseState(
                inputHouseState.getAddress(),
                newOwner);
        txBuilder.addOutputState(outputHouseState, HouseContract.ID);

        HouseContract.Commands.Transfer commandData = new HouseContract.Commands.Transfer();
        List<PublicKey> requiredSigners = ImmutableList.of(
                inputHouseState.getOwner().getOwningKey(), newOwner.getOwningKey());
        txBuilder.addCommand(commandData, requiredSigners);

        txBuilder.verify(getServiceHub());

        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession ownerSession = initiateFlow(newOwner);
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(ownerSession)));


        return subFlow(new FinalityFlow(fullySignedTx, ownerSession));
    }
}
