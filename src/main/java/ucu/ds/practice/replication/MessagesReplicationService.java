package ucu.ds.practice.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ucu.ds.practice.store.Message;
import ucu.ds.practice.store.Messages;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MessagesReplicationService {
    private static final int SEND_RETRY_INTERVAL_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(MessagesReplicationService.class);

    private final Messages messages;

    private final ExecutorService replicationExecutor = Executors.newCachedThreadPool();
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(10);
    
    private final RestTemplate restTemplate = new RestTemplate();

    public MessagesReplicationService(Messages messages) {
        this.messages = messages;
    }


    public void replicateToNodes(MessageReplicationTask task) {
        // Зберігаємо повідомлення локально (у лідера)
        messages.saveMessage(task.getMessage());

        // Розсилаємо іншим - використовуємо replicationExecutor
        replicationExecutor.submit(() -> replicateToFollowers(task));
    }


    private void replicateToFollowers(MessageReplicationTask task) {
        while (!task.isDone()) {
            List<MessageDelivery> inProgressDeliveries = task.getDeliveriesInProgress();
            logger.info("Found {} deliveries in progress for task: {} in status: {}",
                    inProgressDeliveries.size(), task.getMessage(), task.getStatus());
            inProgressDeliveries.stream()
                    .filter(MessageDelivery::isTimeToTrySend)
                    .peek(MessageDelivery::trySend)
                    .peek(delivery -> logger.info("{} sands to node: <{}>. Next send attempt in: {}",
                            task.getMessage(), delivery.getNode().getId(), delivery.getNextSendDt()))
                    // Використовуємо networkExecutor для відправки
                    .forEach(delivery -> networkExecutor.submit(
                            () -> sendMessagesToNode(task.getMessage(), delivery.getNode().getAddress())));
            try {
                Thread.sleep(SEND_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted for {}", task.getMessage());
            }
        }
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