package ucu.ds.practice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InternalRestInterface {

    private static final Logger logger = LoggerFactory.getLogger(InternalRestInterface.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final InternalData internalData;
    private final Nodes nodes;
    private final Messages messages;
    private final MessageReplicationTasks messageReplicationTasks;

    public InternalRestInterface(InternalData internalData, Nodes nodes, Messages messages,
                                 MessageReplicationTasks messageReplicationTasks) {
        this.internalData = internalData;
        this.nodes = nodes;
        this.messages = messages;
        this.messageReplicationTasks = messageReplicationTasks;
    }

    @PostMapping("/acknowledgment")
    public String acknowledgment(@RequestBody String message,
                                 @RequestParam(value = "node_id") String nodeId,
                                 @RequestParam(value = "message_id") Integer messageId) {
        logger.info("Received acknowledgment for message: {} from node <{}>", messageId, nodeId);
        verifyRequestAcknowledgment(nodeId, messageId);

        if ("OK".equals(message)) {
            MessageReplicationTask task = messageReplicationTasks.getTaskByMessageId(messageId);
            if (task != null) {
                task.addNodeAccepted(nodes.getNodeById(nodeId));
                if (task.isDone()) {
                    messageReplicationTasks.removeTask(task);
                }
            }
        }
        return "OK";
    }

    private void verifyRequestAcknowledgment(String nodeId, Integer messageId) {
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Follower cannot receive acknowledgments from clients.");
        }
        if (nodeId == null || messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request parameters 'node_id' and 'message_id' are required.");
        }
        if (nodes.getNodeById(nodeId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request parameter 'node_id' contains unknown node.");
        }
    }

    @PostMapping("/save_message")
    public String saveMessage(@RequestBody Message message) {
        verifyRequestSaveMessage(message);

        waitIfNodeIsSlow(message);

        messages.saveMessage(message);
        logger.info("{} on node <{}> has been successfully saved", message, internalData.getCurrentNodeId());

        sendAcknowledgment(message);
        return "OK";
    }

    private void verifyRequestSaveMessage(Message message) {
        if (internalData.getStatus() == NodeStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is not available.");
        }
        logger.info("Received {} to save on node <{}>", message, internalData.getCurrentNodeId());
        if (internalData.isLeader()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader cannot save messages directly.");
        }
        if (message == null || message.getMessage() == null || message.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty.");
        }
    }

    private void waitIfNodeIsSlow(Message message) {
        if (internalData.getStatus() == NodeStatus.SLOW && internalData.getNodeDelaySec() > 0) {
            logger.info("{} should be saved with delay on node <{}>", message, internalData.getCurrentNodeId());
            try {
                Thread.sleep(internalData.getNodeDelaySec() * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted for {}", message);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Operation interrupted");
            }
        }
    }

    @PostMapping("/health_report")
    public String healthReport(@RequestBody String message,
                               @RequestParam(value = "node_id") String nodeId) {
        verifyRequestHealthReport(nodeId);

        if ("OK".equals(message)) {
            nodes.getNodeById(nodeId).heartBeatAccepted();
        }
        return "OK";
    }

    private void verifyRequestHealthReport(String nodeId) {
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Follower cannot receive health reports.");
        }
        if (nodeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request parameter 'node_id' is required.");
        }
        if (nodes.getNodeById(nodeId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request parameter 'node_id' contains unknown node.");
        }
    }

    private void sendAcknowledgment(Message message) {
        try {
            String url = "http://" + internalData.getLeaderNode() + ":" + internalData.getPort() +"/acknowledgment"
                    + "?node_id=" + internalData.getCurrentNodeId()
                    + "&message_id=" + message.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("OK", headers);

            restTemplate.postForEntity(url, request, String.class);
            logger.info("Acknowledgment sent to leader");
        } catch (RestClientException e) {
            logger.error("Failed to send acknowledgment to leader: {}", e.getMessage());
        }
    }
}