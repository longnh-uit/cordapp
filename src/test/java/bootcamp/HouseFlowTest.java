package bootcamp;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class HouseFlowTest {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        Collections.singletonList(TestCordapp.findCordapp("bootcamp"))
                )
        );
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
    }



    @Test
    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
        HouseRegisterFlowInitiator flow = new HouseRegisterFlowInitiator("12 Le Thanh Tong", nodeB.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());

        HouseTransferFlowInitiator flow2 = new HouseTransferFlowInitiator("12 Le Thanh Tong", nodeA.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future2 = nodeB.startFlow(flow2);

        network.runNetwork();

        SignedTransaction signedTransaction2 = future2.get();
        assertEquals(1, signedTransaction2.getTx().getOutputStates().size());
        TransactionState output2 = signedTransaction2.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output2.getNotary());
    }

    @Test
    public void transactionTest() throws Exception {
        HouseTransferFlowInitiator flow2 = new HouseTransferFlowInitiator("12 Le Thanh Tong", nodeA.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future2 = nodeB.startFlow(flow2);
        SignedTransaction signedTransaction = future2.get();
        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output2 = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output2.getNotary());


    }

    @After
    public void tearDown() {
        network.stopNodes();
    }
}
