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
    private volatile String status;
    private final List<MessageDelivery> deliveries;
    private final int writeConcern;

    private final CountDownLatch latch = new CountDownLatch(1);

    public MessageReplicationTask(Message message, int writeConcern, List<Node> nodes) {
        this.message = message;
        this.writeConcern = writeConcern;
        deliveries = nodes.stream().map(MessageDelivery::new).toList();
        setStatus("NEW");
    }

    public void setStatus(String status) {
        this.status = status;
        logger.info("Task with {} changed status on '{}'", message, status);
    }

    public String getStatus() {
        return status;
    }

    public boolean isConcerned() {
        return "CONCERNED".equals(status) || "DONE".equals(status);
    }

    public boolean isDone() {
        return "DONE".equals(status);
    }

    public void addNodeAccepted(Node node) {
        logger.info("Task with {} has been accepted by node <{}>", message, node.getId());
        synchronized (deliveries) {
            Optional<MessageDelivery> delivery = deliveries.stream()
                    .filter(d -> d.getNode().equals(node))
                    .findFirst();
            if (delivery.isPresent()) {
                delivery.get().delivered();;
            }
            int done = getDeliveriesInStatus("DELIVERED").size();
            if (done == deliveries.size()) {
                setStatus("DONE");
            } else if (done >= writeConcern) {
                setStatus("CONCERNED");
            }
        }
        if (isConcerned()) {
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

    public List<MessageDelivery> getDeliveriesInStatus(String status) {
        synchronized (deliveries) {
            return deliveries.stream().filter(d -> d.getStatus().equals(status)).toList();
        }
    }
}
