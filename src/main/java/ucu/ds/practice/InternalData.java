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
import java.util.stream.Stream;

@Component
public class InternalData {
    private static final String DEFAULT_PORT = "8080";
    private static final Logger logger = LoggerFactory.getLogger(InternalData.class);

    private String status = "FAST";
    private final Set<Message> messages = new ConcurrentSkipListSet<>();
    private final List<MessageReplicationTask> tasks = new CopyOnWriteArrayList<>();

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRole;

    @Value("${NODE_ID:node-leader}")
    private String nodeId;

    @Value("${PEER_NODES:}")
    private String peerNodesRaw;
    private List<Node> externalNodes = Collections.emptyList();
    private Node currentNode;

    @Value("${PORT:" + DEFAULT_PORT + "}")
    private String port;

    @Value("${NODE_DELAY_SEC:0}")
    private Integer nodeDelaySec;

    private final AtomicInteger messageIdGenerator = new AtomicInteger(0);

    @PostConstruct
    public void initPeerNodes() {
        if (peerNodesRaw != null && !peerNodesRaw.isBlank()) {
            currentNode = new Node(nodeId, port);
            externalNodes = Stream.of(peerNodesRaw.split(","))
                    .map(id -> new Node(id, port))
                    .toList();
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

    public Integer getNodeDelaySec() {
        return nodeDelaySec;
    }

    public void setNodeDelaySec(Integer nodeDelaySec) {
        this.nodeDelaySec = nodeDelaySec;
    }

    public void addTask(MessageReplicationTask task) {
        tasks.add(task);
    }
    
    public List<MessageReplicationTask> getTasks() {
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

    public Node getCurrentNode() {
        return currentNode;
    }

    public List<Node> getExternalNodes() {
        return externalNodes;
    }

    public List<Node> getAllNodes() {
        ArrayList<Node> nodes = new ArrayList<>(externalNodes);
        nodes.add(currentNode);
        return nodes;
    }

    public Node getNodeById(String id) {
        return getExternalNodes().stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst().orElse(null);
    }

    public boolean hasNoQuorum() {
        return externalNodes.stream()
                .filter(node -> !node.getHealthStatus().equals("UNHEALTHY")) // відбираємо робочі ноди
                .count() < (getAllNodes().size() / 2);
    }
}