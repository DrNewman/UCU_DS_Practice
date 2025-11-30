package ucu.ds.practice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RestInterface {
    private static final int CLIENT_REQUEST_TIMEOUT_SEC = 100;

    private static final Logger logger = LoggerFactory.getLogger(RestInterface.class);

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
            return "Follower cannot receive new messages from clients.";
        }
        if (message == null || message.isBlank()) {
            return "Message is empty.";
        }
        Message newMessage = new Message(internalData.getNextMessageId(), message);
        MessageDistributionTask task = new MessageDistributionTask(newMessage, writeConcern);
        logger.info("Task with {} on node <{}> has been added to process", newMessage, internalData.getNodeId());
        internalData.addTask(task);

        try {
            boolean completed = task.waitForCompletion(CLIENT_REQUEST_TIMEOUT_SEC);
            if (completed) {
                logger.info("{} on node <{}> has been successfully processed", newMessage, internalData.getNodeId());
                return "OK. Message received and distributed to nodes";
            } else {
                logger.info("{} on node <{}> has been skipped by timeout", newMessage, internalData.getNodeId());
                return "NO. Message received, but distribution timed out";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Operation interrupted";
        }
    }

    @GetMapping("/all_saved_messages")
    public List<Message> getAllSavedMessages() {
        return internalData.getMessages();
    }

    @PostMapping("/save_message")
    public String saveMessage(@RequestBody Message message) {
        logger.info("Received {} to save on node <{}>", message, internalData.getNodeId());
        if (internalData.isLeader()) {
            return "Leader cannot save messages directly.";
        }
        if (message == null || message.getMessage() == null || message.getMessage().isBlank()) {
            return "Message is empty.";
        }
        internalData.saveMessage(message);
        logger.info("{} on node <{}> has been successfully saved", message, internalData.getNodeId());
        return "OK";
    }
}