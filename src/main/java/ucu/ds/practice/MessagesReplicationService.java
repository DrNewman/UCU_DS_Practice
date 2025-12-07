package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MessagesReplicationService {
    private static final Logger logger = LoggerFactory.getLogger(MessagesReplicationService.class);

    @Autowired
    private InternalData internalData;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 1000)
    public void tryToReplicateToFollowers() {
        if (!internalData.isLeader()) {
            return;
        }

        internalData.getTasks().stream()
                .filter(t -> !t.isDone())
                .forEach(this::tryToReplicateToFollowers);
    }

    public void tryToReplicateToFollowers(MessageReplicationTask task) {
        if ("NEW".equals(task.getStatus())) {
            // Зберігаємо повідомлення локально (у лідера)
            task.setStatus("IN_PROGRESS");
            internalData.saveMessage(task.getMessage());
            task.addNodeAccepted(internalData.getCurrentNode());
        }

        // Розсилаємо іншим
        List<MessageDelivery> inProgressDeliveries = task.getDeliveriesInStatus("IN_PROGRESS");
        logger.info("Found {} deliveries in progress for task: {} in status: {}",
                inProgressDeliveries.size(), task.getMessage(), task.getStatus());
        inProgressDeliveries.stream()
                .filter(MessageDelivery::isTimeToTrySend)
                .peek(MessageDelivery::trySend)
                .peek(delivery -> logger.info("{} sands to node: <{}>. Next send attempt in: {}",
                        task.getMessage(), delivery.getNode().getId(), delivery.getNextSendDt()))
                .forEach(delivery -> executorService.submit(
                        () -> sendMessagesToNode(task.getMessage(), delivery.getNode().getAddress())));
    }

    private void sendMessagesToNode(Message message, String nodeAddress) {
        try {
            String url = "http://" + nodeAddress + "/save_message";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Message> request = new HttpEntity<>(message, headers);

            restTemplate.postForEntity(url, request, String.class);
        } catch (RestClientException e) {
            logger.error("Failed to send message to {}: {}", nodeAddress, e.getMessage());
        }
    }
}