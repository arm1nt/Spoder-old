import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {

    private final AtomicInteger runningTasks = new AtomicInteger(0);

    private final AtomicInteger totalTasksRegistered = new AtomicInteger(0);

    private final ExecutorService executorService;

    ThreadPoolManager(int numberOfThreads) {
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }


    /**
     * Takes a task, increments the {@link #runningTasks} task counter and submits it to the ThreadPool.
     *
     * @param runnable Task to be executed
     */
    public void submit(Runnable runnable) {
        totalTasksRegistered.getAndIncrement();
        runningTasks.getAndIncrement();
        executorService.submit(runnable);
    }


    /**
     * Decrement the {@link #runningTasks} counter.
     * If the last task calls this method, the thread pool will be shut down.
     */
    public void decrement() {
        runningTasks.getAndDecrement();

        if (runningTasks.get() == 0) {
            System.out.println("Total tasks registered: " + totalTasksRegistered.get());
            interrupt();
        }
    }


    /**
     * Shuts down the thread pool. If shutting down takes more than 600 Milliseconds, we are exiting.
     */
    public void interrupt() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(600, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(600, TimeUnit.MILLISECONDS)) {
                    System.out.println("Shutting down takes longer than expected.\n Exiting now...");
                    System.exit(0);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
