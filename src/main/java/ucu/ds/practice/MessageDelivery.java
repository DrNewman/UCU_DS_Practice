package ucu.ds.practice;

import java.time.LocalDateTime;
import java.util.List;

public class MessageDelivery {
    private static final List<Integer> DELAY_LIST = List.of(1, 1, 1, 1, 2, 4, 8, 16, 32);

    private String status = "IN_PROGRESS";
    private final String node;
    private LocalDateTime nextSendDt;
    private int sendTryCounter = 0;

    public MessageDelivery(String node) {
        this.node = node;
        nextSendDt = LocalDateTime.now();
    }

    public void trySend() {
        if (sendTryCounter >= DELAY_LIST.size()) {
            status = "CANCELED";
            return;
        }
        nextSendDt = nextSendDt.plusSeconds(DELAY_LIST.get(sendTryCounter));
        sendTryCounter++;
    }

    public String getNode() {
        return node;
    }

    public boolean isTimeToTrySend() {
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
