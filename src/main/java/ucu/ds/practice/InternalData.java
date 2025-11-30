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

    private static final Logger logger = LoggerFactory.getLogger(InternalData.class);

    private final Set<Message> messages = new ConcurrentSkipListSet<>();
    private final List<MessageDistributionTask> tasks = new CopyOnWriteArrayList<>();

    @Value("${NODE_ROLE:LEADER}")
    private String nodeRole;

    @Value("${NODE_ID:node-leader}")
    private String nodeId;

    @Value("${PEER_NODES:}")
    private String peerNodesRaw;
    private List<String> nodes = Collections.emptyList();

    private final AtomicInteger messageIdGenerator = new AtomicInteger(0);

    @PostConstruct
    public void initPeerNodes() {
        if (peerNodesRaw != null && !peerNodesRaw.isBlank()) {
            nodes = List.of(peerNodesRaw.split(","));
        }
        if (isLeader()) {
            logger.info("Leader-node has followers: {}", nodes);
        }
    }

    public int getNextMessageId() {
        return messageIdGenerator.incrementAndGet();
    }

    public void saveMessage(Message message) {
        // Трішки магії для перевірки завдання
        // Якщо повідомлення містить фрагмент "wait" та фрагмент з id поточної ноди, збереження стає на паузу на 60 сек.
        if (message.getMessage().contains("wait") && message.getMessage().contains(nodeId)) {
            logger.info("{} should be saved with delay on node <{}>", message, getNodeId());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted for {}", message);
            }
        }

        messages.add(message);
        logger.info("{} on node <{}> has been saved", message, getNodeId());
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
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