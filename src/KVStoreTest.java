// Main test class
import application.AMOApplication;
import application.KVStore;
import client.Client;
import client.Reply;
import client.Request;
import commands.*;
import results.*;
import server.Server;

import java.io.IOException;

// Main test class
public class KVStoreTest {
    public static void main(String[] args) {
        System.out.println("Starting KV Store Test Application...");

        // Initialize the KV Store application
        KVStore kvStore = new KVStore();

        // Wrap it in an AMO application
        AMOApplication amoApp = new AMOApplication(kvStore);

        // Create a server with the AMO application
        Server server = new Server(amoApp);

        // Create a client connected to the server
        Client client = new Client(1, server, 1000);

        System.out.println("\n--- Running Basic Tests ---");
        runBasicTests(client);

        System.out.println("\n--- Running At-Most-Once Tests ---");
        runAtMostOnceTests(client, server);

        System.out.println("\n--- All tests completed successfully ---");
        client.shutdown();
    }

    private static void runBasicTests(Client client) {
        // Test 1: Put operation
        System.out.println("Test 1: Put operation");
        Result putResult = client.execute(new PutCommand("key1", "value1"));
        assert putResult instanceof PutOk : "Expected PutOk but got " + putResult.getClass().getName();
        System.out.println("Put operation successful");

        // Test 2: Get operation for existing key
        System.out.println("\nTest 2: Get operation for existing key");
        Result getResult = client.execute(new GetCommand("key1"));
        assert getResult instanceof GetResult : "Expected GetResult but got " + getResult.getClass().getName();
        String value = ((GetResult) getResult).getValue();
        assert value.equals("value1") : "Expected 'value1' but got '" + value + "'";
        System.out.println("Get operation returned correct value: " + value);

        // Test 3: Get operation for non-existing key
        System.out.println("\nTest 3: Get operation for non-existing key");
        Result notFoundResult = client.execute(new GetCommand("nonexistent"));
        assert notFoundResult instanceof KeyNotFound : "Expected KeyNotFound but got " + notFoundResult.getClass().getName();
        String notFoundKey = ((KeyNotFound) notFoundResult).getKey();
        assert notFoundKey.equals("nonexistent") : "Expected 'nonexistent' but got '" + notFoundKey + "'";
        System.out.println("Get operation correctly returned KeyNotFound for non-existent key");

        // Test 4: Append operation for existing key
        System.out.println("\nTest 4: Append operation for existing key");
        Result appendResult = client.execute(new AppendCommand("key1", "-appended"));
        assert appendResult instanceof AppendResult : "Expected AppendResult but got " + appendResult.getClass().getName();
        String newValue = ((AppendResult) appendResult).getNewValue();
        assert newValue.equals("value1-appended") : "Expected 'value1-appended' but got '" + newValue + "'";
        System.out.println("Append operation successful, new value: " + newValue);

        // Test 5: Append operation for non-existing key
        System.out.println("\nTest 5: Append operation for non-existing key");
        Result appendNewResult = client.execute(new AppendCommand("key2", "brand-new"));
        assert appendNewResult instanceof AppendResult : "Expected AppendResult but got " + appendNewResult.getClass().getName();
        String appendedValue = ((AppendResult) appendNewResult).getNewValue();
        assert appendedValue.equals("brand-new") : "Expected 'brand-new' but got '" + appendedValue + "'";
        System.out.println("Append operation for new key successful, value: " + appendedValue);
    }

    private static void runAtMostOnceTests(Client client, Server server) {
        // Test 6: At-most-once semantics (duplicate request)
        System.out.println("Test 6: At-most-once semantics (duplicate request)");

        // First operation - put a new key
        long clientId = 1001;
        long requestId = 42;
        Command originalCommand = new PutCommand("duplicate-test", "original-value");
        AMOCommand amoCmd = new AMOCommand(originalCommand, clientId, requestId);
        Request request = new Request(amoCmd);

        // Execute the command
        Reply reply1 = server.handleRequest(request);
        assert reply1.getResult() instanceof AMOResult : "Expected AMOResult";
        AMOResult amoResult1 = (AMOResult) reply1.getResult();
        assert amoResult1.getResult() instanceof PutOk : "Expected PutOk";
        System.out.println("First execution successful");

        // Execute the same command again (should be idempotent)
        Reply reply2 = server.handleRequest(request);
        assert reply2.getResult() instanceof AMOResult : "Expected AMOResult";
        AMOResult amoResult2 = (AMOResult) reply2.getResult();
        assert amoResult2.getResult() instanceof PutOk : "Expected PutOk";
        System.out.println("Second execution successful (returned cached result)");

        // Verify the value wasn't changed twice
        Result getResult = client.execute(new GetCommand("duplicate-test"));
        assert getResult instanceof GetResult : "Expected GetResult";
        String value = ((GetResult) getResult).getValue();
        assert value.equals("original-value") : "Expected 'original-value' but got '" + value + "'";
        System.out.println("Verified that key contains correct value: " + value);

        // Test 7: Multiple client sessions
        System.out.println("\nTest 7: Multiple client sessions");

        // Create two more clients
        Client client2 = new Client(2, server, 1000);
        Client client3 = new Client(3, server, 1000);

        // Client 2 sets a key
        Result client2Result = client2.execute(new PutCommand("multi-client", "client2-value"));
        assert client2Result instanceof PutOk : "Expected PutOk";
        System.out.println("Client 2 put operation successful");

        // Client 3 updates the key
        Result client3Result = client3.execute(new AppendCommand("multi-client", "-client3-append"));
        assert client3Result instanceof AppendResult : "Expected AppendResult";
        String client3Value = ((AppendResult) client3Result).getNewValue();
        assert client3Value.equals("client2-value-client3-append") :
                "Expected 'client2-value-client3-append' but got '" + client3Value + "'";
        System.out.println("Client 3 append operation successful, new value: " + client3Value);

        // Original client reads the key
        Result clientReadResult = client.execute(new GetCommand("multi-client"));
        assert clientReadResult instanceof GetResult : "Expected GetResult";
        String finalValue = ((GetResult) clientReadResult).getValue();
        assert finalValue.equals("client2-value-client3-append") :
                "Expected 'client2-value-client3-append' but got '" + finalValue + "'";
        System.out.println("Original client successfully read the updated value: " + finalValue);

        // Clean up clients
        client2.shutdown();
        client3.shutdown();
    }
}