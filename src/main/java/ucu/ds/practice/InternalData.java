package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InternalData {
    private static final String DEFAULT_PORT = "8080";
    private static final Logger logger = LoggerFactory.getLogger(InternalData.class);

    private String status = "FAST";

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRole;

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
            status = "SLOW";
        }
    }

    public int getNextMessageId() {
        return messageIdGenerator.incrementAndGet();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
        return "LEADER".equals(nodeRole);
    }

    public boolean isFollower() {
        return "FOLLOWER".equals(nodeRole);
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public String getLeaderNode() {
        return leaderNode;
    }
}