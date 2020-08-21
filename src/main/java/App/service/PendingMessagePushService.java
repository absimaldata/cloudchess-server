package App.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
@Log4j2
public class PendingMessagePushService {

    private Deque<String> pendingMessageQueue = new ArrayDeque<>();

    public void pushToClient(String line) {
        try {
            pendingMessageQueue.offer(line);
            log.info("Pushed to pending message queue");
        } catch (Exception e) {
            log.error("SQS Queue is not ready to accept line: " + line, e);
        }
    }

    public List<String> pollMessages() {
        List<String> messages = new ArrayList<>();
        int size = pendingMessageQueue.size();
        for(int i = 0; i < size; i++) {
            messages.add(pendingMessageQueue.poll());
        }
        if(!CollectionUtils.isEmpty(messages)) {
            log.info("Going to send lines to client. " + messages);
        }
        return messages;
    }

    public void clearPendingMessages() {
        this.pendingMessageQueue = new ArrayDeque<>();
    }
}