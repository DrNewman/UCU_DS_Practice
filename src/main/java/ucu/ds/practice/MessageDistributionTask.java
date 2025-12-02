package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageDistributionTask {
    private static final Logger logger = LoggerFactory.getLogger(MessageDistributionTask.class);

    private final Message message;
    private volatile String status;
    private final List<String> nodesAccepted = new ArrayList<>();
    private final int nodesAcceptedThreshold;

    private final CountDownLatch latch = new CountDownLatch(1);

    public MessageDistributionTask(Message message, int nodesAcceptedThreshold) {
        this.message = message;
        this.nodesAcceptedThreshold = nodesAcceptedThreshold;
        setStatus("NEW");
    }

    public void setStatus(String status) {
        this.status = status;
        logger.info("Task with {} changed status on '{}'", message, status);
    }

    public String getStatus() {
        return status;
    }

    public boolean isDone() {
        return "DONE".equals(status);
    }

    public void addNodeAccepted(String node) {
        logger.info("Task with {} has been accepted by node <{}>", message, node);
        synchronized (nodesAccepted) {
            if (nodesAccepted.contains(node)) {
                return;
            }
            nodesAccepted.add(node);
            if (!isDone() && nodesAccepted.size() >= nodesAcceptedThreshold) {
                setStatus("DONE");
            }
        }
        if (isDone()) {
            latch.countDown();
        }
    }

    /**
     * Метод блокує виконання, поки задача не буде виконана або не вийде час очікування.
     * @param timeoutSeconds час очікування в секундах
     * @return true якщо задача виконана успішно, false якщо вийшов тайм-аут
     */
    public boolean waitForCompletion(long timeoutSeconds) throws InterruptedException {
        logger.info("Waiting for completion of the task with {} started", message);
        boolean result = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        logger.info("Waiting for completion of the task with {} ended. Result: {}", message, result);
        return result;
    }

    public Message getMessage() {
        return message;
    }
}
