package ucu.ds.practice.cooperation;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ucu.ds.practice.InternalData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Component
public class Nodes {
    private static final Logger logger = LoggerFactory.getLogger(Nodes.class);

    private final InternalData internalData;

    public Nodes(InternalData internalData) {
        this.internalData = internalData;
    }

    @Value("${FOLLOWER_NODES:}")
    private String followerNodesRaw;
    private List<Node> followerNodes = Collections.emptyList();
    private Node currentNode;

    @PostConstruct
    public void init() {
        if (internalData.isLeader()) {
            if (followerNodesRaw != null && !followerNodesRaw.isBlank()) {
                currentNode = new Node(internalData.getCurrentNodeId(), internalData.getPort());
                followerNodes = Stream.of(followerNodesRaw.split(","))
                        .map(id -> new Node(id, internalData.getPort()))
                        .toList();
            }
            logger.info("Leader-node has followers: {}", followerNodes);
        }
    }

    public List<Node> getFollowerNodes() {
        return followerNodes;
    }

    public List<Node> getAllNodes() {
        ArrayList<Node> nodes = new ArrayList<>(followerNodes);
        nodes.add(currentNode);
        return nodes;
    }

    public Node getNodeById(String id) {
        return getFollowerNodes().stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst().orElse(null);
    }

    public boolean hasNoQuorum() {
        return followerNodes.stream()
                .filter(node -> node.getHealthStatus() != NodeHealthStatus.UNHEALTHY) // відбираємо робочі ноди
                .count() < (getAllNodes().size() / 2);
    }
}
