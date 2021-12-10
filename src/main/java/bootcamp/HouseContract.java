package bootcamp;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

public class HouseContract implements Contract {

    public static final String ID = "bootcamp.HouseContract";

    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final List<PublicKey> requiredSigners = command.getSigners();

        if (command.getValue() instanceof Commands.Register) {

            // "Shape" constraints.
            if (tx.getInputStates().size() != 0)
                throw new IllegalArgumentException("Registration transaction must have no inputs.");
            if (tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Registration transaction must have one output.");
            if (tx.outputsOfType(HouseState.class).size() != 1)
                throw new IllegalArgumentException("Registration transaction output must be a HouseState.");

            // Content constraints.
            final ContractState outputState = tx.getOutput(0);
            HouseState houseState = (HouseState) outputState;
            if (houseState.getAddress().length() <= 3)
                throw new IllegalArgumentException("Address must be longer than 3 characters");

            // Required signer constraints.
            Party owner = houseState.getOwner();
            PublicKey ownersKey = owner.getOwningKey();
            if (!(requiredSigners.contains(ownersKey)))
                throw new IllegalArgumentException("Owner of house must sign registration.");

        } else if (command.getValue() instanceof Commands.Transfer) {

            // "Shape" constraints.
            if (tx.getInputStates().size() != 1)
                throw new IllegalArgumentException("Transfer transaction must have one input.");
            if (tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Transfer transaction must have one output.");
            if (tx.inputsOfType(HouseState.class).size() != 1)
                throw new IllegalArgumentException("Transfer transaction inHoput must be a HouseState.");
            if (tx.outputsOfType(HouseState.class).size() != 1)
                throw new IllegalArgumentException("Transfer transaction output must be a HouseState.");


            // Content constraints.
            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            if (!(input instanceof HouseState))
                throw new IllegalArgumentException("Input must be a HouseState.");
            if (!(output instanceof HouseState))
                throw new IllegalArgumentException("Output must be a HouseState.");

            HouseState inputHouse = (HouseState) input;
            HouseState outputHouse = (HouseState) output;

            if (!(inputHouse.getAddress().equals(outputHouse.getAddress())))
                throw new IllegalArgumentException("In a transfer, the address can't change.");
            if (inputHouse.getOwner().equals(outputHouse.getOwner()))
                throw new IllegalArgumentException("In a transfer, the owner must change.");

            // Signer constraints.
            Party inputOwner = inputHouse.getOwner();
            Party outputOwner = outputHouse.getOwner();

            if (!(requiredSigners.contains(inputOwner.getOwningKey())))
                throw new IllegalArgumentException("Current owner must sign transfer.");
            if (!requiredSigners.contains(outputOwner.getOwningKey()))
                throw new IllegalArgumentException("New owner must sign transfer.");

        } else {
            throw new IllegalArgumentException("Command type not recognised.");
        }
    }
    public interface Commands extends CommandData {
        class Register implements Commands { }
        class Transfer implements Commands { }
    }
}
