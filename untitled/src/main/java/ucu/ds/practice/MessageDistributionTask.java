package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageDistributionTask {
    private static final int NODES_CAPACITY = 3;

    private static final Logger logger = LoggerFactory.getLogger(MessageDistributionTask.class);

    private final String message;
    private volatile String status; // volatile для видимості змін між потоками
    private final List<String> nodesAccepted = new ArrayList<>();
    private final int nodesAcceptedThreshold = NODES_CAPACITY;

    private final CountDownLatch latch = new CountDownLatch(1);

    public MessageDistributionTask(String message) {
        this.message = message;
        setStatus("NEW");
    }

    public void setStatus(String status) {
        this.status = status;
        logger.info("Task with message {} changed status on: {}", message, status);
    }

    public String getStatus() {
        return status;
    }

    public boolean isDone() {
        return "DONE".equals(status);
    }

    public void addNodeAccepted(String node) {
        logger.info("Task with message {} has been accepted by node: {}", message, node);
        synchronized (nodesAccepted) {
            if (nodesAccepted.contains(node)) {
                return;
            }
            nodesAccepted.add(node);
            if (!isDone() && nodesAccepted.size() >= nodesAcceptedThreshold) {
                setStatus("DONE");
                latch.countDown(); 
            }
        }
    }

    /**
     * Метод блокує виконання, поки задача не буде виконана або не вийде час очікування.
     * @param timeoutSeconds час очікування в секундах
     * @return true якщо задача виконана успішно, false якщо вийшов тайм-аут
     */
    public boolean waitForCompletion(long timeoutSeconds) throws InterruptedException {
        logger.info("Waiting for completion of the task with message '{}' started", message);
        boolean result = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        logger.info("Waiting for completion of the task with message '{}' ended. Result: {}", message, result);
        return result;
    }

    public String getMessage() {
        return message;
    }
}
