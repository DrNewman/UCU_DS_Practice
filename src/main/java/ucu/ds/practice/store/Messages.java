package ucu.ds.practice.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ucu.ds.practice.InternalData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class Messages {
    private static final Logger logger = LoggerFactory.getLogger(Messages.class);

    private final InternalData internalData;

    public Messages(InternalData internalData) {
        this.internalData = internalData;
    }

    private final Set<Message> messages = new ConcurrentSkipListSet<>();

    public void saveMessage(Message message) {

        messages.add(message);
        logger.info("{} on node <{}> has been saved", message, internalData.getCurrentNodeId());
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public List<Message> getTotalOrderedMessages() {
        List<Message> allMessages = getAllMessages();
        if (allMessages.isEmpty() || allMessages.get(allMessages.size() - 1).getId() == allMessages.size()) {
            return allMessages;
        }
        if (allMessages.get(0).getId() != 1) {
            return Collections.emptyList();
        }
        int counter = 1;
        while (allMessages.get(counter - 1).getId() == counter) {
            counter++;
        }
        return allMessages.subList(0, counter);
    }
}
