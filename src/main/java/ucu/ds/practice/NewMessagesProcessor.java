package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class NewMessagesProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NewMessagesProcessor.class);

    @Autowired
    private InternalData internalData;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();
    
    private int processedTasksCount = 0;

    @Scheduled(fixedRate = 1000)
    public void distribute() {
        if (!internalData.isLeader()) {
            return;
        }

        int currentTasksCount = internalData.getTasks().size();
        if (currentTasksCount > processedTasksCount) {
            int newCount = currentTasksCount - processedTasksCount;
            logger.info("New messages ({}) to process on node: {} have been found", newCount, internalData.getNodeId());
            
            List<MessageDistributionTask> notProcessedTasks = internalData.getTasks().subList(processedTasksCount, currentTasksCount);
            notProcessedTasks.forEach(this::processTask);
            
            processedTasksCount = currentTasksCount;
            logger.info("New messages processing on node: {} has been completed", internalData.getNodeId());
        }
    }

    private void processTask(MessageDistributionTask task) {
        task.setStatus("IN_PROGRESS");
        // Зберігаємо повідомлення локально (у лідера)
        internalData.saveMessage(task.getMessage());
        task.addNodeAccepted(internalData.getNodeId());
    
        // Розсилаємо іншим
        internalData.getNodes().forEach(node -> sendMessagesToNode(task, node));
    }

    private void sendMessagesToNode(MessageDistributionTask task, String nodeAddress) {
        executorService.submit(() -> {
            try {
                String url = "http://" + nodeAddress + "/save_message";
            
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                HttpEntity<String> request = new HttpEntity<>(task.getMessage(), headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody().equals("OK")) {
                    task.addNodeAccepted(nodeAddress);
                }
            } catch (RestClientException e) {
                logger.error("Failed to send message to {}: {}", nodeAddress, e.getMessage());
            }
        });
    }
}