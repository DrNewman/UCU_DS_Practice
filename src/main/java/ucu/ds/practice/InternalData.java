package ucu.ds.practice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InternalData {
    private static final String DEFAULT_PORT = "8080";

    private NodeStatus status = NodeStatus.FAST;

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRoleRaw;
    private NodeRole nodeRole;

    @Value("${NODE_ID:node-leader}")
    private String currentNodeId;

    @Value("${PORT:" + DEFAULT_PORT + "}")
    private String port;

    @Value("${LEADER_NODE:node-leader}")
    private String leaderNode;

    @Value("${NODE_DELAY_SEC:0}")
    private Integer nodeDelaySec;

    private final AtomicInteger messageIdGenerator = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        if (!nodeDelaySec.equals(0)) {
            status = NodeStatus.SLOW;
        }
        nodeRole = NodeRole.valueOf(nodeRoleRaw);
    }

    public int getNextMessageId() {
        return messageIdGenerator.incrementAndGet();
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public String getPort() {
        return port;
    }

    public Integer getNodeDelaySec() {
        return nodeDelaySec;
    }

    public void setNodeDelaySec(Integer nodeDelaySec) {
        this.nodeDelaySec = nodeDelaySec;
    }

    public boolean isLeader() {
        return nodeRole == NodeRole.LEADER;
    }

    public boolean isFollower() {
        return nodeRole == NodeRole.FOLLOWER;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public String getLeaderNode() {
        return leaderNode;
    }
}