package ucu.ds.practice;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MessageReplicationTasks {
    private final List<MessageReplicationTask> tasks = new CopyOnWriteArrayList<>();

    public void addTask(MessageReplicationTask task) {
        tasks.add(task);
    }

    public void removeTask(MessageReplicationTask task) {
        tasks.remove(task);
    }

    public MessageReplicationTask getTaskByMessageId(Integer messageId) {
        return tasks.stream()
                .filter(t -> messageId.equals(t.getMessage().getId()))
                .findFirst()
                .orElse(null);
    }
}
