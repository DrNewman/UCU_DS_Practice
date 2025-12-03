package ucu.ds.practice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RestInterface {
    private static final int CLIENT_REQUEST_TIMEOUT_SEC = 100;

    private static final Logger logger = LoggerFactory.getLogger(RestInterface.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private final InternalData internalData;

    public RestInterface(InternalData internalData) {
        this.internalData = internalData;
    }

    @PostMapping("/new_message")
    public String newMessage(@RequestBody String message,
                             @RequestParam(value = "write_concern", defaultValue = "3") Integer writeConcern) {
        logger.info("Received new message: {} to node <{}> with write_concern: {}",
                message, internalData.getNodeId(), writeConcern);
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Follower cannot receive new messages from clients.");
        }
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty.");
        }

        Message newMessage = new Message(internalData.getNextMessageId(), message);
        List<String> nodes = new ArrayList<>(internalData.getExternalNodes());
        nodes.add(internalData.getNodeId());
        MessageDistributionTask task = new MessageDistributionTask(newMessage, writeConcern, nodes);
        logger.info("Task with {} on node <{}> has been added to process", newMessage, internalData.getNodeId());
        internalData.addTask(task);

        try {
            boolean completed = task.waitForCompletion(CLIENT_REQUEST_TIMEOUT_SEC);
            if (completed) {
                logger.info("{} on node <{}> has been successfully processed", newMessage, internalData.getNodeId());
                return "OK. Message received and distributed to nodes";
            } else {
                logger.info("{} on node <{}> has been skipped by timeout", newMessage, internalData.getNodeId());
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,
                        "Message received, but distribution timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Operation interrupted");
        }
    }

    @PostMapping("/acknowledgment")
    public String acknowledgment(@RequestBody String message,
                                 @RequestParam(value = "node_id") String nodeId,
                                 @RequestParam(value = "message_id") Integer messageId) {
        logger.info("Received acknowledgment for message: {} from node <{}>", messageId, nodeId);
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Follower cannot receive acknowledgments from clients.");
        }
        if (nodeId == null || messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Request parameters 'node_id' and 'message_id' are required.");
        }
        if ("OK".equals(message)) {
            internalData.getTasks().stream()
                    .filter(t -> !t.isDone())
                    .filter(t -> messageId.equals(t.getMessage().getId()))
                    .findFirst()
                    .ifPresent(task -> task.addNodeAccepted(nodeId));
        }
        return "OK";
    }

    @GetMapping("/all_saved_messages")
    public List<Message> getAllSavedMessages() {
        return internalData.getTotalOrderedMessages();
    }

    @PostMapping("/save_message")
    public String saveMessage(@RequestBody Message message) {
        if (internalData.getStatus().equals("PAUSED")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is not available.");
        }
        logger.info("Received {} to save on node <{}>", message, internalData.getNodeId());
        if (internalData.isLeader()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader cannot save messages directly.");
        }
        if (message == null || message.getMessage() == null || message.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty.");
        }
        internalData.saveMessage(message);
        logger.info("{} on node <{}> has been successfully saved", message, internalData.getNodeId());

        sendAcknowledgment(message);
        return "OK";
    }

    @PostMapping("/command")
    public String command(@RequestBody String notUsed,
                          @RequestParam(value = "command") String command,
                          @RequestParam(value = "delay_time", defaultValue = "0") Integer delayTime) {
        logger.info("Received : '{}' for node <{}>", command, internalData.getNodeId());
        if (internalData.isLeader()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader cannot receive commands.");
        }
        switch (command) {
            case "pause": internalData.setStatus("PAUSED"); break;
            case "slow": internalData.setStatus("SLOW");
                if (delayTime != 0) {
                    internalData.setNodeDelaySec(delayTime);
                }
                break;
            case "fast": internalData.setStatus("FAST"); break;
            default: throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown command. Supported commands: pause, slow, fast");
        }
        return "OK";
    }

    private void sendAcknowledgment(Message message) {
        try {
            String url = "http://node-leader:" + internalData.getPort() +"/acknowledgment" +
                    "?node_id=" + internalData.getNodeId()
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