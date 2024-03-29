package App.service.tasks;

import App.config.ThreadSignallingConfiguration;
import App.service.PendingMessagePushService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Deque;

@AllArgsConstructor
@Log4j2
public class ClientPushTask implements Runnable {

    private Process process;
    private PendingMessagePushService pendingMessagePushService;
    private ThreadSignallingConfiguration threadSignallingConfiguration;
    private Deque<String> buffer;

    @Override
    public void run() {
        Thread.currentThread().setPriority(8);
        try(BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            boolean timeout = false;
            while (true) {
                if(threadSignallingConfiguration.isClientPushTask()) {
                    threadSignallingConfiguration.setClientPushTask(false);
                    emptyBufferAndSendToQueue();
                    log.info("Closing tasks");
                    processOutput.close();
                    return;
                }
                emptyBufferAndSendToQueue();
                String line = null;
                if(!timeout) {
                    String readLine = null;
                    try {
                        readLine = processOutput.readLine();
                    } catch (IOException e) {
                        log.error("IO Exception, emptying the buffer", e);
                        emptyBufferAndSendToQueue();
                    }
                    line = readLine;
                }

                try {
                    if(line == null) {
                        continue;
                    }
                    if(line.equals("")) {
                        buffer.offer("<emptyline>");
                    } else {
                        buffer.offer(line);
                    }
                    timeout = false;
                } catch (Exception t) {
                    timeout = true;
                    emptyBufferAndSendToQueue();
                }
            }
        } catch (Exception e) {
            log.error("Closing tasks", e);
            threadSignallingConfiguration.setClientPushTask(false);
        }
    }

    private void emptyBufferAndSendToQueue() {
        if(buffer.size() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            while (this.buffer.size() != 0) {
                stringBuilder.append(buffer.poll());
                stringBuilder.append("\n");
            }
            pendingMessagePushService.pushToClient(stringBuilder.toString());
        }
    }
}
