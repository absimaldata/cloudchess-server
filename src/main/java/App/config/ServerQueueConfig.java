package App.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Log4j2
public class ServerQueueConfig {
    private BlockingQueue<String> serverQueue = new LinkedBlockingQueue<>();

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
        this.serverQueue = new LinkedBlockingQueue<>();
    }
}
