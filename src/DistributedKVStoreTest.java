//import application.AMOApplication;
//import application.KVStore;
//import client.Client;
//import client.Reply;
//import client.Request;
//import commands.*;
//import results.*;
//import server.Server;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.util.concurrent.TimeUnit;
//
//// Distributed KV Store Test for networked client and server
//public class DistributedKVStoreTest {
//    private static final int SERVER_PORT = 8000;
//    private static final int CLIENT_PORT_1 = 8001;
//    private static final int CLIENT_PORT_2 = 8002;
//    private static final int CLIENT_PORT_3 = 8003;
//
//    public static void main(String[] args) {
//        System.out.println("Starting Distributed KV Store Test Application...");
//
//        try {
//            String localIp = InetAddress.getLocalHost().getHostAddress();
//            System.out.println("Running on local IP: " + localIp);
//
//            // Initialize the KV Store application
//            KVStore kvStore = new KVStore();
//
//            // Wrap it in an AMO application
//            AMOApplication amoApp = new AMOApplication(kvStore);
//
//            // Create and start the server
//            Server server = new Server(amoApp, SERVER_PORT);
//            server.start();
//            System.out.println("Server started on port " + SERVER_PORT);
//
//            // Give the server a moment to initialize
//            TimeUnit.SECONDS.sleep(1);
//
//            // Create and start clients on different ports
//            Client client = new Client(1, localIp, SERVER_PORT, CLIENT_PORT_1, 3000);
//            client.start();
//            System.out.println("Client 1 started on port " + CLIENT_PORT_1);
//
//            System.out.println("\n--- Running Basic Tests ---");
//            runBasicTests(client, localIp);
//
//            System.out.println("\n--- Running At-Most-Once Tests ---");
//            runAtMostOnceTests(client, server, localIp);
//
//            System.out.println("\n--- All tests completed successfully ---");
//
//            // Shutdown clients and server
//            client.shutdown();
//            server.stop();
//
//        } catch (Exception e) {
//            System.err.println("Test failed with exception: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private static void runBasicTests(Client client, String localIp) {
//        try {
//            // Test 1: Put operation
//            System.out.println("Test 1: Put operation");
//            Result putResult = client.execute(new PutCommand("key1", "value1"));
//            assert putResult instanceof PutOk : "Expected PutOk but got " + putResult.getClass().getName();
//            System.out.println("Put operation successful");
//
//            // Give some time for network communication
//            TimeUnit.MILLISECONDS.sleep(500);
//
//            // Test 2: Get operation for existing key
//            System.out.println("\nTest 2: Get operation for existing key");
//            Result getResult = client.execute(new GetCommand("key1"));
//            assert getResult instanceof GetResult : "Expected GetResult but got " + getResult.getClass().getName();
//            String value = ((GetResult) getResult).getValue();
//            assert value.equals("value1") : "Expected 'value1' but got '" + value + "'";
//            System.out.println("Get operation returned correct value: " + value);
//
//            // Test 3: Get operation for non-existing key
//            System.out.println("\nTest 3: Get operation for non-existing key");
//            Result notFoundResult = client.execute(new GetCommand("nonexistent"));
//            assert notFoundResult instanceof KeyNotFound : "Expected KeyNotFound but got " + notFoundResult.getClass().getName();
//            String notFoundKey = ((KeyNotFound) notFoundResult).getKey();
//            assert notFoundKey.equals("nonexistent") : "Expected 'nonexistent' but got '" + notFoundKey + "'";
//            System.out.println("Get operation correctly returned KeyNotFound for non-existent key");
//
//            // Test 4: Append operation for existing key
//            System.out.println("\nTest 4: Append operation for existing key");
//            Result appendResult = client.execute(new AppendCommand("key1", "-appended"));
//            assert appendResult instanceof AppendResult : "Expected AppendResult but got " + appendResult.getClass().getName();
//            String newValue = ((AppendResult) appendResult).getNewValue();
//            assert newValue.equals("value1-appended") : "Expected 'value1-appended' but got '" + newValue + "'";
//            System.out.println("Append operation successful, new value: " + newValue);
//
//            // Test 5: Append operation for non-existing key
//            System.out.println("\nTest 5: Append operation for non-existing key");
//            Result appendNewResult = client.execute(new AppendCommand("key2", "brand-new"));
//            assert appendNewResult instanceof AppendResult : "Expected AppendResult but got " + appendNewResult.getClass().getName();
//            String appendedValue = ((AppendResult) appendNewResult).getNewValue();
//            assert appendedValue.equals("brand-new") : "Expected 'brand-new' but got '" + appendedValue + "'";
//            System.out.println("Append operation for new key successful, value: " + appendedValue);
//
//        } catch (InterruptedException e) {
//            System.err.println("Test interrupted: " + e.getMessage());
//        }
//    }
//
//    private static void runAtMostOnceTests(Client client, Server server, String localIp) {
//        try {
//            // Test 6: Multiple client sessions
//            System.out.println("\nTest 6: Multiple client sessions");
//
//            // Create two more clients on different ports
//            Client client2 = new Client(2, localIp, SERVER_PORT, CLIENT_PORT_2, 3000);
//            client2.start();
//            System.out.println("Client 2 started on port " + CLIENT_PORT_2);
//
//            Client client3 = new Client(3, localIp, SERVER_PORT, CLIENT_PORT_3, 3000);
//            client3.start();
//            System.out.println("Client 3 started on port " + CLIENT_PORT_3);
//
//            // Give clients time to start
//            TimeUnit.SECONDS.sleep(1);
//
//            // Client 2 sets a key
//            Result client2Result = client2.execute(new PutCommand("multi-client", "client2-value"));
//            assert client2Result instanceof PutOk : "Expected PutOk but got " + client2Result.getClass().getName();
//            System.out.println("Client 2 put operation successful");
//
//            // Give time for network communication
//            TimeUnit.MILLISECONDS.sleep(500);
//
//            // Client 3 updates the key
//            Result client3Result = client3.execute(new AppendCommand("multi-client", "-client3-append"));
//            assert client3Result instanceof AppendResult : "Expected AppendResult but got " + client3Result.getClass().getName();
//            String client3Value = ((AppendResult) client3Result).getNewValue();
//            assert client3Value.equals("client2-value-client3-append") :
//                    "Expected 'client2-value-client3-append' but got '" + client3Value + "'";
//            System.out.println("Client 3 append operation successful, new value: " + client3Value);
//
//            // Give time for network communication
//            TimeUnit.MILLISECONDS.sleep(500);
//
//            // Original client reads the key
//            Result clientReadResult = client.execute(new GetCommand("multi-client"));
//            assert clientReadResult instanceof GetResult : "Expected GetResult but got " + clientReadResult.getClass().getName();
//            String finalValue = ((GetResult) clientReadResult).getValue();
//            assert finalValue.equals("client2-value-client3-append") :
//                    "Expected 'client2-value-client3-append' but got '" + finalValue + "'";
//            System.out.println("Original client successfully read the updated value: " + finalValue);
//
//            // Test 7: At-most-once semantics (duplicate request)
//            System.out.println("\nTest 7: At-most-once semantics (simulated duplicate request)");
//
//            // Execute a command twice with the same request ID
//            long clientId = 1001;
//            long requestId = 42;
//
//            // Create a special client that allows us to control request IDs
//            Client specialClient = new Client(clientId, localIp, SERVER_PORT, 8004, 3000) {
//                @Override
//                public Result execute(Command cmd) {
//                    // Override to use our specific request ID
//                    AMOCommand amoCmd = new AMOCommand(cmd, clientId, requestId);
//                    Request request = new Request(amoCmd);
//
//                    // Send request and wait for response
//                    return super.execute(amoCmd);
//                }
//            };
//            specialClient.start();
//
//            // Give client time to start
//            TimeUnit.SECONDS.sleep(1);
//
//            // First execution
//            Result result1 = specialClient.execute(new PutCommand("duplicate-test", "original-value"));
//            assert result1 instanceof PutOk : "Expected PutOk but got " + result1.getClass().getName();
//            System.out.println("First execution successful");
//
//            // Give time for network communication
//            TimeUnit.MILLISECONDS.sleep(500);
//
//            // Second execution (should be idempotent)
//            Result result2 = specialClient.execute(new PutCommand("duplicate-test", "this-should-be-ignored"));
//            assert result2 instanceof PutOk : "Expected PutOk but got " + result2.getClass().getName();
//            System.out.println("Second execution successful (should have returned cached result)");
//
//            // Give time for network communication
//            TimeUnit.MILLISECONDS.sleep(500);
//
//            // Verify the value wasn't changed twice
//            Result getResult = client.execute(new GetCommand("duplicate-test"));
//            assert getResult instanceof GetResult : "Expected GetResult but got " + getResult.getClass().getName();
//            String value = ((GetResult) getResult).getValue();
//            assert value.equals("original-value") : "Expected 'original-value' but got '" + value + "'";
//            System.out.println("Verified that key contains correct value: " + value);
//
//            // Clean up clients
//            client2.shutdown();
//            client3.shutdown();
//            specialClient.shutdown();
//
//        } catch (InterruptedException e) {
//            System.err.println("Test interrupted: " + e.getMessage());
//        }
//    }
//}