package App.service.tasks;

import App.config.ServerEngineConfig;
import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.ProcessManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Log4j2
public class ProcessFlusherTask implements Runnable {

    private BufferedWriter processWriter;
    private ServerQueueConfig serverQueueConfig;
    private ThreadSignallingConfiguration threadSignallingConfiguration;
    private ProcessManagerService processManagerService;
    private ServerEngineConfig serverEngineConfig;

    public ProcessFlusherTask(BufferedWriter processWriter, ServerQueueConfig serverQueueConfig, ThreadSignallingConfiguration threadSignallingConfiguration, ProcessManagerService processManagerService, ServerEngineConfig serverEngineConfig) {
        this.processWriter = processWriter;
        this.serverQueueConfig = serverQueueConfig;
        this.threadSignallingConfiguration = threadSignallingConfiguration;
        this.processManagerService = processManagerService;
        this.serverEngineConfig = serverEngineConfig;
    }

    private String lastLine;
    private String lastGoPonder;

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
                            if(line.contains("setoption")) {
                                populateOptions(line);
                            }
                            /*if(line.contains("ponderhit")) {
                                waitForReadStop();
                                line = "stop";
                            }
                            if(line.contains("setoption name Ponder")) {
                                continue;
                            }

                            String finalLine = line;
                            if(line.contains("go ponder") && lastLine != null) {
                                lastGoPonder = line;
                                finalLine = lastLine.substring(0, lastLine.length() - 5) + "\n" + lastGoPonder;
                            }
                            log.info("Going to write line: " + finalLine);*/
                            processWriter.write(line + "\n");
                            processWriter.flush();
                            if(line.contains("position")) {
                                lastLine = line;
                            }
                        } catch (IOException io) {
                            log.error("Error on received line: " + line, io);
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

    private void populateOptions(String line) {
        try {
            log.info("populating config....");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("d:\\Engines\\server.json"));
            String[] tokens = line.split(" ");
            StringBuilder name = new StringBuilder();
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equalsIgnoreCase("name")) {
                    for (int j = i + 1; j < tokens.length; j++) {
                        if (!tokens[j].equalsIgnoreCase("value")) {
                            name.append(tokens[j]);
                            name.append(" ");
                        } else {
                            break;
                        }
                    }
                }
                if (tokens[i].equalsIgnoreCase("value")) {
                    for (int j = i + 1; j < tokens.length; j++) {
                        value.append(tokens[j]);
                        value.append(" ");
                    }
                }
            }
            this.serverEngineConfig.getOptionNameValueMap().put(name.toString().trim(), value.toString().trim());
            ObjectMapper objectMapper = new ObjectMapper();
            bufferedWriter.write(objectMapper.writeValueAsString(this.serverEngineConfig));
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            log.error("Error populating options", e);
        }
    }

    private String correctTime(String line) {
        try {
            String[] tokens = line.split(" ");
            String wtime = null;
            String btime = null;
            for (int i = 0; i < tokens.length; i++) {
                if (!StringUtils.isEmpty(tokens[i])) {
                    if (tokens[i].equalsIgnoreCase("wtime")) {
                        wtime = tokens[i + 1];
                    }
                    if (tokens[i].equalsIgnoreCase("btime")) {
                        btime = tokens[i + 1];
                    }
                }
            }

            int wtimeI, btimeI;

            if (!StringUtils.isEmpty(wtime) && !StringUtils.isEmpty(btime)) {
                wtimeI = Integer.valueOf(wtime);
                btimeI = Integer.valueOf(btime);
                wtimeI -= 2000;
                btimeI -= 2000;
                return "go wtime " + wtimeI + " winc 2000 btime " + btimeI + " binc 2000";
            } else {
                return line;
            }
        } catch (Exception e) {
            return line;
        }
    }

    public void waitForReadStop() throws Exception {
        this.threadSignallingConfiguration.setStopRead(true);
        while (!threadSignallingConfiguration.isStopState()) {
            Thread.sleep(10);
        }
    }
}
