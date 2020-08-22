package App.service.tasks;

import App.service.PendingMessagePushService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;

@Log4j2
@AllArgsConstructor
public class StdErrReaderTask implements Runnable {

    private BufferedReader bufferedReader;
    private PendingMessagePushService pendingMessagePushService;

    @Override
    public void run() {
        while(true) {
            try {
                String line = bufferedReader.readLine();
                if(line != null) {
                    pendingMessagePushService.pushToClient(line);
                    log.error("Error from process: " + line);
                }
                Thread.sleep(10);
            } catch (Exception e) {
                log.error("Exception in reading error stream", e.getMessage());
                return;
            }
        }
    }
}
