package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MessagesDistributor {
    private static final Logger logger = LoggerFactory.getLogger(MessagesDistributor.class);

    @Autowired
    private InternalData internalData;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();

    public void processTask(MessageDistributionTask task) {
        task.setStatus("IN_PROGRESS");
        // Зберігаємо повідомлення локально (у лідера)
        internalData.saveMessage(task.getMessage());
        task.addNodeAccepted(internalData.getNodeId());
    
        // Розсилаємо іншим
        internalData.getNodes().forEach(node -> executorService.submit(() -> sendMessageToNode(task, node)));
    }

    private void sendMessageToNode(MessageDistributionTask task, String nodeAddress) {
        try {
            String url = "http://" + nodeAddress + "/save_message";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Message> request = new HttpEntity<>(task.getMessage(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && "OK".equals(response.getBody())) {
                task.addNodeAccepted(nodeAddress);
            }
        } catch (RestClientException e) {
            logger.error("Failed to send message to {}: {}", nodeAddress, e.getMessage());
        }
    }
}