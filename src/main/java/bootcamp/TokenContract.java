package bootcamp;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;    
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.security.PublicKey;
import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final List<PublicKey> requiredSigners = command.getSigners();

        if (command.getValue() instanceof Commands.Issue) {

            // "Shape" constraints
            if (tx.getInputs().size() != 0)
                throw new IllegalArgumentException("Must have no inputs.");
            if (tx.getOutputs().size() != 1)
                throw new IllegalArgumentException("Must have one output.");
            if (tx.outputsOfType(TokenState.class).size() != 1)
                throw new IllegalArgumentException("Output must be a TokenState");

            // Contents constraints
            final TokenState tokenStateOutput = tx.outputsOfType(TokenState.class).get(0);
            if (tokenStateOutput.getAmount() <= 0)
                throw new IllegalArgumentException("The amount must be greater than 0.");

            // Required signers constraints.
            if (!(requiredSigners.contains(tokenStateOutput.getIssuer().getOwningKey())))
                throw new IllegalArgumentException("Issuer must sign the contract");

        } else throw new IllegalArgumentException("Unrecognised command.");

    }


    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}
