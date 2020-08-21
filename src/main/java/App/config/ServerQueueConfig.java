package App.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
@Log4j2
public class ServerQueueConfig {
    private Deque<String> serverQueue = new ArrayDeque<>();

    public void offer(String line) {
        try {
            serverQueue.offer(line);
        } catch (Exception e) {
            log.error("Exception in pushing to queue", e);
        }
    }

    public String poll() {
        return serverQueue.poll();
    }

    public String peek() {
        return serverQueue.peek();
    }

    public void empty() {
        this.serverQueue = new ArrayDeque<>();
    }
}
