package App.service.tasks;

import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.ProcessManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

@AllArgsConstructor
@Log4j2
public class ProcessFlusherTask implements Runnable {

    private Process process;
    private ServerQueueConfig serverQueueConfig;
    private ThreadSignallingConfiguration threadSignallingConfiguration;
    private ProcessManagerService processManagerService;

    @Override
    public void run() {
        try(BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            while (true) {
                if(threadSignallingConfiguration.isStopAnalysis()) {
                    processWriter.write("stop\n");
                    processWriter.flush();
                    threadSignallingConfiguration.setStopAnalysis(false);
                    processManagerService.updateProcessState(ProcessState.STARTED);
                }

                if(threadSignallingConfiguration.isShutdown()) {
                    // Sleep this thread so that Client push task can close first
                    Thread.sleep(100);
                    processWriter.write("quit\n");
                    processWriter.flush();
                    log.info("Closing tasks");
                    processWriter.close();
                    return;
                }
                String line = serverQueueConfig.peek();
                if (line != null) {
                    if(processManagerService.getProcessState() != ProcessState.RUNNING) {
                        processManagerService.updateProcessState(ProcessState.RUNNING);
                    }
                    try {
                        processWriter.write(line + "\n");
                        processWriter.flush();
                        serverQueueConfig.poll();
                    } catch(IOException io) {
                        log.error("Error on received line: " + line, io);
                        Thread.sleep(100);
                    }
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            log.info("Closing tasks", e);
        }
    }
}
