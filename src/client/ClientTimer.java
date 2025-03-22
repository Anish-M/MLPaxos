package client;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientTimer {
    private final ScheduledExecutorService scheduler;
    private final Runnable task;
    private final long timeoutMs;

    public ClientTimer(ScheduledExecutorService scheduler, Runnable task, long timeoutMs) {
        this.scheduler = scheduler;
        this.task = task;
        this.timeoutMs = timeoutMs;
    }

    public void start() {
        scheduler.schedule(task, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void cancel() {
        // Cancel the timer if needed
    }
}
