package App.service;

import App.config.ServerEngineConfig;
import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.tasks.ClientPushTask;
import App.service.tasks.ProcessFlusherTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Log4j2
public class ProcessManagerService {

    @Autowired
    private ServerEngineConfig serverEngineConfig;

    @Autowired
    private ServerQueueConfig serverQueueConfig;

    @Autowired
    private PendingMessagePushService pendingMessagePushService;

    @Autowired
    private ConfigFileAutoLoader configFileAutoLoader;

    @Autowired
    private HeartBeatService heartBeatService;

    @Autowired
    private ThreadSignallingConfiguration threadSignallingConfiguration;

    private Process process;
    private ProcessState processState = ProcessState.CLOSED;

    private ExecutorService processFlusherService = Executors.newFixedThreadPool(1);
    private ExecutorService clientPushServiceExecutor= Executors.newFixedThreadPool(1);


    public Process getProcess() {
        return this.process;
    }

    public ProcessState getProcessState() {
        return this.processState;
    }

    public synchronized void unloadProcess() {
        this.processState = ProcessState.CLOSING;
        closeProcessThreads();

        if(process != null) {
            process.destroyForcibly();
            this.process = null;
        }
        // Empty the queues
        serverQueueConfig.empty();
        purgeQueue();
        this.processState = ProcessState.CLOSED;
    }

    public synchronized void closeProcessThreads() {
        try {
            this.threadSignallingConfiguration.setShutdown(true);
            Thread.sleep(500);
        } catch (Exception e) {
            log.error("Exception in closing process threads");
        }
    }

    public synchronized void reload() {
        try {
            if(this.processState == ProcessState.STARTED || this.processState == ProcessState.CLOSING) {
                return;
            }

            if(this.processState == ProcessState.RUNNING) {
                threadSignallingConfiguration.setStopAnalysis(true);
                Thread.sleep(100);
            }

            this.processState = ProcessState.CLOSING;
            log.info("Reloading");
            if(process != null) {
                unloadProcess();
            }

            this.processFlusherService = Executors.newFixedThreadPool(1);
            this.clientPushServiceExecutor = Executors.newFixedThreadPool(1);

            Runtime rt = Runtime.getRuntime();
            String command = serverEngineConfig.getEngineName();
            this.process = rt.exec(command);

            ClientPushTask clientPushTask = new ClientPushTask(process, pendingMessagePushService, threadSignallingConfiguration, new ArrayDeque<>());
            clientPushServiceExecutor.submit(clientPushTask);

            BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            ProcessFlusherTask processFlusherTask = new ProcessFlusherTask(processWriter, serverQueueConfig, threadSignallingConfiguration, this);
            processFlusherService.submit(processFlusherTask);

            Thread.sleep(1000);
            this.processState = ProcessState.STARTED;
            log.info("Reload Complete");

        } catch (Exception e) {
            log.error("Exception in reloading the engine", e);
        }
    }

    private void purgeQueue() {
        try {
            pendingMessagePushService.clearPendingMessages();
        } catch (Exception e) {
            log.error("Error purging the queue", e);
        }
    }

    public synchronized void stopAnalysis() {
        this.threadSignallingConfiguration.setStopAnalysis(true);
        this.processState = ProcessState.SLEEPING;
    }

    public void updateProcessState(ProcessState processState) {
        synchronized (this) {
            this.processState = processState;
        }
    }

    public String isProcessStarted() {
        return this.processState.name();
    }
}
