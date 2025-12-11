package ucu.ds.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class HeartBeatGenerationService {
    private static final int HEART_BEAT_INTERVAL_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(HeartBeatGenerationService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final InternalData internalData;

    public HeartBeatGenerationService(InternalData internalData) {
        this.internalData = internalData;
    }

    @Scheduled(fixedDelay = HEART_BEAT_INTERVAL_MS)
    public void heartBeat() {
        if (internalData.isFollower() && internalData.getStatus() != NodeStatus.PAUSED) {
            sendBeat();
        }
    }

    private void sendBeat() {
        try {
            String url = "http://" + internalData.getLeaderNode() + ":" + internalData.getPort() +"/health_report"
                    + "?node_id=" + internalData.getCurrentNodeId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("OK", headers);

            restTemplate.postForEntity(url, request, String.class);
        } catch (RestClientException e) {
            logger.error("Failed to send heart beat to leader: {}", e.getMessage());
        }
    }
}
