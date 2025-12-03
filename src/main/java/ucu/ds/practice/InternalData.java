package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.web.server.ResponseStatusException;

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
    private final Set<Message> messages = new ConcurrentSkipListSet<>();
    private final List<MessageDistributionTask> tasks = new CopyOnWriteArrayList<>();

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRole;

    @Value("${NODE_ID:node-leader}")
    private String nodeId;

    @Value("${PEER_NODES:}")
    private String peerNodesRaw;
    private List<String> externalNodes = Collections.emptyList();

    @Value("${PORT:" + DEFAULT_PORT + "}")
    private String port;

    @Value("${NODE_DELAY_SEC:0}")
    private Integer nodeDelaySec;

    private final AtomicInteger messageIdGenerator = new AtomicInteger(0);

    @PostConstruct
    public void initPeerNodes() {
        if (peerNodesRaw != null && !peerNodesRaw.isBlank()) {
            externalNodes = List.of(peerNodesRaw.split(","));
        }
        if (!nodeDelaySec.equals(0)) {
            status = "SLOW";
        }
        if (isLeader()) {
            logger.info("Leader-node has followers: {}", externalNodes);
        }
    }

    public int getNextMessageId() {
        return messageIdGenerator.incrementAndGet();
    }

    public void saveMessage(Message message) {
        if (status.equals("SLOW") && nodeDelaySec > 0) {
            logger.info("{} should be saved with delay on node <{}>", message, getNodeId());
            try {
                Thread.sleep(nodeDelaySec * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted for {}", message);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Operation interrupted");
            }
        }

        messages.add(message);
        logger.info("{} on node <{}> has been saved", message, getNodeId());
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public List<Message> getTotalOrderedMessages() {
        List<Message> allMessages = getAllMessages();
        if (allMessages.isEmpty() || allMessages.get(allMessages.size() - 1).getId() == allMessages.size()) {
            return allMessages;
        }
        if (allMessages.get(0).getId() != 1) {
            return Collections.emptyList();
        }
        int counter = 1;
        while (allMessages.get(counter - 1).getId() == counter) {
            counter++;
        }
        return allMessages.subList(0, counter);
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

    public void setNodeDelaySec(Integer nodeDelaySec) {
        this.nodeDelaySec = nodeDelaySec;
    }

    public void addTask(MessageDistributionTask task) {
        tasks.add(task);
    }
    
    public List<MessageDistributionTask> getTasks() {
        return tasks;
    }

    public boolean isLeader() {
        return "LEADER".equals(nodeRole);
    }

    public boolean isFollower() {
        return "FOLLOWER".equals(nodeRole);
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<String> getExternalNodes() {
        return externalNodes;
    }
}