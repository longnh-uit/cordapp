package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class HouseRegisterFlowInitiator extends FlowLogic<SignedTransaction> {
    private final String address;
    private final Party newOwner;

    public HouseRegisterFlowInitiator(String address, Party newOwner) {
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
        // Extract all the `HouseState`s from the vault
        List<StateAndRef<HouseState>> houseStateAndRefs = getServiceHub().getVaultService().queryBy(HouseState.class).getStates();

        // Check if the address was already exist
        if (houseStateAndRefs.stream().anyMatch(houseStateAndRef -> {
            HouseState houseState = houseStateAndRef.getState().getData();
            return houseState.getAddress().equals(address);
        })) throw new IllegalArgumentException("The house is already purchased");

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Build a transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        // Add output HouseState to the transaction
        HouseState outputHouseState = new HouseState(
                address,
                newOwner);
        txBuilder.addOutputState(outputHouseState, HouseContract.ID);

        // Add the Register command
        HouseContract.Commands.Register commandData = new HouseContract.Commands.Register();
        List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), newOwner.getOwningKey());
        txBuilder.addCommand(commandData, requiredSigners);

        // Check if the transaction builder meets contracts of the output state.
        txBuilder.verify(getServiceHub());

        // Sign the transaction and convert into a `SignedTransaction`
        SignedTransaction SignedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession ownerSession = initiateFlow(newOwner);
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(SignedTx, singletonList(ownerSession)));


        return subFlow(new FinalityFlow(fullySignedTx, singletonList(ownerSession)));
    }
}
