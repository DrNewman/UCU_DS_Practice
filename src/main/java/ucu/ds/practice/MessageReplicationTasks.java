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

    public List<MessageReplicationTask> getTasks() {
        return tasks;
    }
}
