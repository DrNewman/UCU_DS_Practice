package ucu.ds.practice.rest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ucu.ds.practice.*;
import ucu.ds.practice.NodeStatus;
import ucu.ds.practice.cooperation.Nodes;
import ucu.ds.practice.replication.MessageReplicationTask;
import ucu.ds.practice.replication.MessageReplicationTasks;
import ucu.ds.practice.replication.MessagesReplicationService;
import ucu.ds.practice.store.Message;
import ucu.ds.practice.store.Messages;

import java.util.List;

@RestController
public class ExternalRestInterface {
    private static final int CLIENT_REQUEST_TIMEOUT_SEC = 100;

    private static final Logger logger = LoggerFactory.getLogger(ExternalRestInterface.class);

    private final InternalData internalData;
    private final Nodes nodes;
    private final Messages messages;
    private final MessageReplicationTasks messageReplicationTasks;
    private final MessagesReplicationService messagesReplicationService;

    public ExternalRestInterface(InternalData internalData, Nodes nodes, Messages messages,
                                 MessageReplicationTasks messageReplicationTasks,
                                 MessagesReplicationService messagesReplicationService) {
        this.internalData = internalData;
        this.nodes = nodes;
        this.messages = messages;
        this.messageReplicationTasks = messageReplicationTasks;
        this.messagesReplicationService = messagesReplicationService;
    }

    @PostMapping("/new_message")
    public String newMessage(@RequestBody String message,
                             @RequestParam(value = "write_concern", defaultValue = "3") Integer writeConcern) {
        logger.info("Received new message: {} to node <{}> with write_concern: {}",
                message, internalData.getCurrentNodeId(), writeConcern);
        verifyRequestNewMessage(message);

        Message newMessage = new Message(internalData.getNextMessageId(), message);
        MessageReplicationTask task = new MessageReplicationTask(newMessage, writeConcern, nodes.getFollowerNodes());
        logger.info("Task with {} on node <{}> has been added to process", newMessage, internalData.getCurrentNodeId());
        messageReplicationTasks.addTask(task);
        messagesReplicationService.replicateToNodes(task);

        if (writeConcern > 1) {
            waitForWriteConcernAchievement(task);
        }
        return "OK. Message received and distributed to %d nodes".formatted(writeConcern);

    }

    private void verifyRequestNewMessage(String message) {
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Follower cannot receive new messages from clients.");
        }
        if (nodes.hasNoQuorum()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "New messages saving is temporarily unavailable. Please try again later.");
        }
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty.");
        }
    }

    private void waitForWriteConcernAchievement(MessageReplicationTask task) {
        try {
            boolean completed = task.waitForCompletion(CLIENT_REQUEST_TIMEOUT_SEC);
            if (completed) {
                logger.info("{} on node <{}> has been successfully processed", task.getMessage(), internalData.getCurrentNodeId());
            } else {
                logger.info("{} on node <{}> has been skipped by timeout", task.getMessage(), internalData.getCurrentNodeId());
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,
                        "Message received, but distribution timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Operation interrupted");
        }
    }

    @GetMapping("/all_saved_messages")
    public List<Message> getAllSavedMessages() {
        return messages.getTotalOrderedMessages();
    }

    @GetMapping("/health")
    public String getHealth() {
        verifyRequestGetHealth();
        return nodes.getFollowerNodes().stream()
                .map(node -> "Node <%s> is '%s'. Last heart-beat was %s"
                        .formatted(node.getId(), node.getHealthStatus(), node.getLastHeartbeat().toString()))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private void verifyRequestGetHealth() {
        if (internalData.isFollower()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Follower cannot receive health report requests.");
        }
    }

    @PostMapping("/command")
    public String command(@RequestBody String notUsed,
                          @RequestParam(value = "command") String command,
                          @RequestParam(value = "delay_time", defaultValue = "0") Integer delayTime) {
        logger.info("Received : '{}' for node <{}>", command, internalData.getCurrentNodeId());
        verifyRequestCommand();

        switch (command) {
            case "pause": internalData.setStatus(NodeStatus.PAUSED); break;
            case "slow": internalData.setStatus(NodeStatus.SLOW);
                if (delayTime != 0) {
                    internalData.setNodeDelaySec(delayTime);
                }
                break;
            case "fast": internalData.setStatus(NodeStatus.FAST); break;
            default: throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown command. Supported commands: pause, slow, fast");
        }
        return "OK";
    }

    private void verifyRequestCommand() {
        if (internalData.isLeader()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader cannot receive commands.");
        }
    }
}