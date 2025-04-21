import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Controller {
    private static final int n = 3; // Number of servers
    private static final int rounds = 20000; // Number of rounds
    private static final int interval = 5000; // Interval in milliseconds

    public static void main(String[] args) {
        try {
            int leader = -1; 
            Random random = new Random();

            // clear the failures file before starting
            // This ensures that we start with a clean slate for logging failures
            // Clear the contents of the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("failures.txt"))) {
                writer.write(""); // Clear the file content
            } catch (IOException e) {
                System.err.println("Error clearing failures.txt: " + e.getMessage());
            }

            // clear the leader file to ensure we start fresh
            // This ensures that we start with a clean slate for logging failures
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("leader.txt"))) {
                writer.write(""); // Clear the file content
            } catch (IOException e) {
                System.err.println("Error clearing leader.txt: " + e.getMessage());
            }

            // clear the tracking_failures.txt file to ensure we start fresh
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("tracking_failures.txt"))) {
                writer.write(""); // Clear the file content
            } catch (IOException e) {
                System.err.println("Error clearing tracking_failures.txt: " + e.getMessage());
            }

            for (int i = 0; i < rounds; i++) {
                leader = readLeaderFromFile("leader.txt");
                int failureType = random.nextInt(2); // 0 for leader, 1 for adjacent link
                if (failureType == 0) {
                    // Fail the leader
                    writeFailureToFile("failures.txt", "Leader " + leader + " failed");
                    // Log the failure in tracking_failures.txt
                    writeFailureToFile("tracking_failures.txt", getExactTime() + " Leader " + leader + " failed", true);
                } else {
                    // Fail an adjacent link
                    int adjacentLink = random.nextBoolean() ? leader - 1 : leader + 1;
                    if (adjacentLink < 0) adjacentLink = n - 1; // Wrap around to the last server if leader is 0
                    if (adjacentLink >= n) adjacentLink = 0;
                    writeFailureToFile("failures.txt", "Link " + leader + " - " + adjacentLink + " failed");
                    // Log the failure in tracking_failures.txt
                    writeFailureToFile("tracking_failures.txt", getExactTime() + " Link " + leader + " - " + adjacentLink + " failed", true);
                }

                // Simulate failure for 5 seconds
                Thread.sleep(interval);

                // Recover from failure
                writeFailureToFile("failures.txt", "Recovery");

                // Wait for 5 seconds before the next round
                Thread.sleep(interval);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int readLeaderFromFile(String fileName) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        if (content == null || content.trim().isEmpty()) {
            // If the file is empty or null, return a default leader
            // This should not happen in a controlled environment
            return 1; // Default to leader 1 if file is empty
        }
        return Integer.parseInt(content.trim());
    }

    // append to file method 
    private static void writeFailureToFile(String fileName, String message, boolean append) throws IOException {
        // This method is used to write failure messages to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, append))) {
            writer.write(message);
            writer.newLine();
        }
    }
    private static void writeFailureToFile(String fileName, String message) throws IOException {

        // clear the file first
        // This ensures that we start with a clean slate for logging failures
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(""); // Clear the file content
        } catch (IOException e) {
            System.err.println("Error clearing failures.txt: " + e.getMessage());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(message);
            writer.newLine();
        }
    }
    
    private static String getExactTime() {
        // Define the format HH:MM:SS:MM
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SS");
        // Get the current date and time
        Date now = new Date();
        // Format the date to a string
        return '[' + formatter.format(now) + "] "; // Return the formatted time string
    }
} 
