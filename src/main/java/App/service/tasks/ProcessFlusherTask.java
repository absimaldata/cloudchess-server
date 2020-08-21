package App.service.tasks;

import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.ProcessManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.IOException;

@AllArgsConstructor
@Log4j2
public class ProcessFlusherTask implements Runnable {

    private BufferedWriter processWriter;
    private ServerQueueConfig serverQueueConfig;
    private ThreadSignallingConfiguration threadSignallingConfiguration;
    private ProcessManagerService processManagerService;

    @Override
    public void run() {
        while (true) {
            try {
                if (threadSignallingConfiguration.isProcessFlusherTask()) {
                    threadSignallingConfiguration.setProcessFlusherTask(false);
                    if(processManagerService.getProcessState() == ProcessState.CLOSING || processManagerService.getProcessState() == ProcessState.STARTED || processManagerService.getProcessState() == ProcessState.RUNNING) {
                        processWriter.write("quit\n");
                        processWriter.flush();
                    }
                    log.info("Closing process flusher");
                    threadSignallingConfiguration.setProcessFlusherTask(false);
                    processManagerService.updateProcessState(ProcessState.CLOSED);
                    processWriter.close();
                    return;
                }

                if (processManagerService.getProcessState() == ProcessState.STARTED || processManagerService.getProcessState() == ProcessState.RUNNING) {
                    String line = serverQueueConfig.poll();
                    if (line != null) {
                        if (processManagerService.getProcessState() != ProcessState.RUNNING) {
                            processManagerService.updateProcessState(ProcessState.RUNNING);
                        }
                        try {
                            log.info("Going to write line: " +line);
                            processWriter.write(line + "\n");
                            processWriter.flush();
                        } catch (IOException io) {
                            log.error("Error on received line: " + line, io);
                            processWriter.close();
                            this.threadSignallingConfiguration.setProcessFlusherTask(false);
                            return;
                        }
                    }
                }
                Thread.sleep(10);
            } catch (Exception e) {
                log.info("Exception in while loop", e);
                this.threadSignallingConfiguration.setProcessFlusherTask(false);
                return;
            }
        } // while
    }
}
