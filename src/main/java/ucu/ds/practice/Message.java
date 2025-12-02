package ucu.ds.practice;

public class Message implements Comparable<Message> {
    private int id;
    private String message;

    public Message() {
    }

    public Message(int id, String message) {
        this.id = id;
        this.message = message;
    }
    public int getId() {
        return id;
    }
    public String getMessage() {
        return message;
    }

    @Override
    public int compareTo(Message o) {
        return Integer.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", message='" + message + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message1 = (Message) o;
        return id == message1.id;
    }
}
