package ucu.ds.practice.replication;

import ucu.ds.practice.cooperation.Node;
import ucu.ds.practice.cooperation.NodeHealthStatus;

import java.time.LocalDateTime;

public class MessageDelivery {
    private static final int BASIC_DELAY = 1;
    private static final int MAX_DELAY = 32;

    private MessageDeliveryStatus status = MessageDeliveryStatus.IN_PROGRESS;
    private final Node node;
    private LocalDateTime nextSendDt;
    private int previousDelay = BASIC_DELAY;

    public MessageDelivery(Node node) {
        this.node = node;
        nextSendDt = LocalDateTime.now();
    }

    public void trySend() {
        if (node.getHealthStatus() == NodeHealthStatus.HEALTHY) {
            nextSendDt = nextSendDt.plusSeconds(BASIC_DELAY);
            previousDelay = BASIC_DELAY;
        }
        if (node.getHealthStatus() == NodeHealthStatus.SUSPECTED) {
            int delay = Integer.min(previousDelay * 2, MAX_DELAY);
            nextSendDt = nextSendDt.plusSeconds(delay);
            previousDelay = delay;
        }
    }

    public Node getNode() {
        return node;
    }

    public boolean isTimeToTrySend() {
        if (node.getHealthStatus() == NodeHealthStatus.UNHEALTHY) {
            return false;
        }
        return LocalDateTime.now().isAfter(nextSendDt);
    }

    public LocalDateTime getNextSendDt() {
        return nextSendDt;
    }

    public boolean isInProgress() {
        return status == MessageDeliveryStatus.IN_PROGRESS;
    }

    public void delivered() {
        this.status = MessageDeliveryStatus.DELIVERED;
    }
}
