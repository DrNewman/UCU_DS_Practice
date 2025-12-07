package ucu.ds.practice;

import java.time.LocalDateTime;

public class Node {
    private final  String id;
    private String port;
    private LocalDateTime lastHeartbeat;

    public Node(String id, String port) {
        this.id = id;
        this.port = port;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public String getHealthStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (lastHeartbeat.isBefore(now.minusSeconds(60))) {
            return "UNHEALTHY";
        }
        if (lastHeartbeat.isBefore(now.minusSeconds(3))) {
            return "SUSPECTED";
        }
        return "HEALTHY";
    }

    public void heartBeatAccepted() {
        lastHeartbeat = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return id + ":" + port;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

//    public void setHealthStatus(String healthStatus) {
//        this.healthStatus = healthStatus;
//    }

    @Override
    public String toString() {
        return "Node{" + "id=" + id + ", healthStatus=" + getHealthStatus() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node1 = (Node) o;
        return id.equals(node1.id);
    }
}
