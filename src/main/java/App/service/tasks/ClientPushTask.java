package App.service.tasks;

import App.config.ThreadSignallingConfiguration;
import App.service.PendingMessagePushService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@AllArgsConstructor
@Log4j2
public class ClientPushTask implements Runnable {

    private Process process;
    private PendingMessagePushService pendingMessagePushService;
    private ThreadSignallingConfiguration threadSignallingConfiguration;
    private Deque<String> buffer;

    @Override
    public void run() {
        try(BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            CompletableFuture<String> future = null;
            boolean timeout = false;
            while (true) {
                if(threadSignallingConfiguration.isStopRead()) {
                    threadSignallingConfiguration.setStopState(true);
                    Thread.sleep(50);
                    continue;
                }
                threadSignallingConfiguration.setStopState(false);
                if(threadSignallingConfiguration.isClientPushTask()) {
                    threadSignallingConfiguration.setClientPushTask(false);
                    emptyBufferAndSendToQueue();
                    log.info("Closing tasks");
                    processOutput.close();
                    return;
                }
                if(buffer.size() == 30) {
                    emptyBufferAndSendToQueue();
                }
                String line = null;
                if(!timeout) {
                    future = CompletableFuture.supplyAsync(() -> {
                        String readLine = null;
                        try {
                            readLine = processOutput.readLine();
                        } catch (IOException e) {
                            log.error("IO Exception, emptying the buffer", e);
                            emptyBufferAndSendToQueue();
                            return null;
                        }
                        return readLine;
                    });
                }

                try {
                    line = future.get(5, TimeUnit.MILLISECONDS);
                    if(line == null) {
                        continue;
                    }
                    if(line.equals("")) {
                        buffer.offer("<emptyline>");
                    } else {
                        buffer.offer(line);
                    }
                    timeout = false;
                } catch (TimeoutException t) {
                    timeout = true;
                    emptyBufferAndSendToQueue();
                    Thread.sleep(10);
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
                String line = buffer.poll();
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            pendingMessagePushService.pushToClient(stringBuilder.toString());
        }
    }
}
