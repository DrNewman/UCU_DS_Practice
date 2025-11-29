package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InternalData {

    private static final Logger logger = LoggerFactory.getLogger(InternalData.class);

    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final List<MessageDistributionTask> tasks = new CopyOnWriteArrayList<>();

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRole;

    @Value("${NODE_ID:node-leader}")
    private String nodeId;

    @Value("${PEER_NODES:}")
    private String peerNodesRaw;
    private List<String> nodes = Collections.emptyList();

    @PostConstruct
    public void initPeerNodes() {
        if (peerNodesRaw != null && !peerNodesRaw.isBlank()) {
            nodes = List.of(peerNodesRaw.split(","));
        }
        if (isLeader()) {
            logger.info("Leader-node has followers: {}", nodes);
        }
    }

    public void saveMessage(String message) {
        // Трішки магії для перевірки завдання
        if (message.contains("wait") && message.contains(nodeId)) {
            logger.info("The message: '{}' should be saved with delay on node: {}", message, getNodeId());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted for message: {}", message);
            }
        }

        messages.add(message);
        logger.info("The message: '{}' on node: {} has been saved", message, getNodeId());
    }

    public List<String> getMessages() {
        return messages;
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

    public List<String> getNodes() {
        return nodes;
    }
}