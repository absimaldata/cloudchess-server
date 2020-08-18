package App.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

@Component
@Log4j2
public class ServerQueueConfig {
    private BlockingQueue<String> serverQueue = new ArrayBlockingQueue<>(1000000, true);

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

    public void empty() {
        this.serverQueue = new ArrayBlockingQueue<>(1000000, true);
    }
}
