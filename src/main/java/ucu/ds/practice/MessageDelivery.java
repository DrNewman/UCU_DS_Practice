package ucu.ds.practice;

import java.time.LocalDateTime;
import java.util.List;

public class MessageDelivery {
    private static final int BASIC_DELAY = 1;
    private static final int MAX_DELAY = 32;

    private String status = "IN_PROGRESS";
    private final Node node;
    private LocalDateTime nextSendDt;
    private int previousDelay = BASIC_DELAY;

    public MessageDelivery(Node node) {
        this.node = node;
        nextSendDt = LocalDateTime.now();
    }

    public void trySend() {
        if (node.getHealthStatus().equals("HEALTHY")) {
            nextSendDt = nextSendDt.plusSeconds(BASIC_DELAY);
            previousDelay = BASIC_DELAY;
        }
        if (node.getHealthStatus().equals("SUSPECTED")) {
            int delay = Integer.min(previousDelay * 2, MAX_DELAY);
            nextSendDt = nextSendDt.plusSeconds(delay);
            previousDelay = delay;
        }
    }

    public Node getNode() {
        return node;
    }

    public boolean isTimeToTrySend() {
        if (node.getHealthStatus().equals("UNHEALTHY")) {
            return false;
        }
        return LocalDateTime.now().isAfter(nextSendDt);
    }

    public LocalDateTime getNextSendDt() {
        return nextSendDt;
    }

    public String getStatus() {
        return status;
    }

    public void delivered() {
        this.status = "DELIVERED";
    }
}
