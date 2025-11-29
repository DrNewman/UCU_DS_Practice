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
    public String newMessage(@RequestBody String message) {
        logger.info("Received new message: {} to node: {}", message, internalData.getNodeId());
        if (internalData.isFollower()) {
            return "Follower cannot receive new messages from clients.";
        }
        if (message == null || message.isBlank()) {
            return "Message is empty.";
        }
        MessageDistributionTask task = new MessageDistributionTask(message);
        logger.info("Task with message: {} on node: {} has been added to process", message, internalData.getNodeId());
        internalData.addTask(task);
        
        try {
            boolean completed = task.waitForCompletion(CLIENT_REQUEST_TIMEOUT_SEC);
            if (completed) {
                logger.info("Message: {} on node: {} has been successfully processed", message, internalData.getNodeId());
                return "Message received and distributed to all nodes";
            } else {
                logger.info("Message: {} on node: {} has been skipped by timeout", message, internalData.getNodeId());
                return "Message received, but distribution timed out (not all nodes responded)";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Operation interrupted";
        }
    }

    @GetMapping("/all_saved_messages")
    public List<String> getAllSavedMessages() {
        return internalData.getMessages();
    }

    @PostMapping("/save_message")
    public String saveMessage(@RequestBody String message) {
        logger.info("Received message: {} to save on node: {}", message, internalData.getNodeId());
        if (internalData.isLeader()) {
            return "Leader cannot save messages directly.";
        }
        if (message == null || message.isBlank()) {
            return "Message is empty.";
        }
        internalData.saveMessage(message);
        logger.info("Message: {} on node: {} has been successfully saved", message, internalData.getNodeId());
        return "OK";
    }
}