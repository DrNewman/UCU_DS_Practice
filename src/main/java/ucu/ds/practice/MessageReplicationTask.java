package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageReplicationTask {
    private static final Logger logger = LoggerFactory.getLogger(MessageReplicationTask.class);

    private final Message message;
    private TaskStatus status;
    private final List<MessageDelivery> deliveries;

    private final CountDownLatch latch;

    public MessageReplicationTask(Message message, int writeConcern, List<Node> nodes) {
        this.message = message;
        deliveries = nodes.stream().map(MessageDelivery::new).toList();
        latch = new CountDownLatch(writeConcern - 1); // мінус leader-нода, на яку реплікувати не треба
        setStatus(TaskStatus.IN_PROGRESS);
    }

    public synchronized void setStatus(TaskStatus status) {
        this.status = status;
        logger.info("Task with {} changed status on '{}'", message, status);
    }

    public synchronized TaskStatus getStatus() {
        return status;
    }

    public synchronized boolean isDone() {
        return status == TaskStatus.DONE;
    }

    public synchronized void addNodeAccepted(Node node) {
        logger.info("Task with {} has been accepted by node <{}>", message, node.getId());
        Optional<MessageDelivery> delivery = deliveries.stream()
                .filter(d -> d.getNode().equals(node))
                .findFirst();
        if (delivery.isPresent() && delivery.get().isInProgress()) {
            delivery.get().delivered();
            latch.countDown();
        }
        if (getDeliveriesInProgress().isEmpty()) {
            setStatus(TaskStatus.DONE);
        }
    }

    /**
     * Метод блокує виконання, поки повідомлення не буде розповсюджене на достатню кількість нод
     * або не вийде час очікування.
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

    public synchronized List<MessageDelivery> getDeliveriesInProgress() {
        return deliveries.stream().filter(MessageDelivery::isInProgress).toList();
    }
}
