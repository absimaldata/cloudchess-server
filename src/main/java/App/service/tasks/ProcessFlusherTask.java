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
                if (processManagerService.getProcessState() == ProcessState.STARTED || processManagerService.getProcessState() == ProcessState.RUNNING) {
                    if (threadSignallingConfiguration.isStopAnalysis()) {
                        processWriter.write("stop\n");
                        processWriter.flush();
                        threadSignallingConfiguration.setStopAnalysis(false);
                        processManagerService.updateProcessState(ProcessState.STARTED);
                    }

                    if (threadSignallingConfiguration.isShutdown()) {
                        processWriter.write("quit\n");
                        processWriter.flush();
                        log.info("Closing tasks");
                        processWriter.close();
                        return;
                    }

                    String line = serverQueueConfig.poll();
                    if (line != null) {
                        if (processManagerService.getProcessState() != ProcessState.RUNNING) {
                            processManagerService.updateProcessState(ProcessState.RUNNING);
                        }
                        try {
                            processWriter.write(line + "\n");
                            processWriter.flush();
                        } catch (IOException io) {
                            log.error("Error on received line: " + line, io);
                            return;
                        }
                    }
                }
                Thread.sleep(10);
            } catch (Exception e) {
                log.info("Exception in while loop", e);
                return;
            }
        } // while
    }
}
