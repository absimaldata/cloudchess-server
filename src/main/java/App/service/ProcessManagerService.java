package App.service;

import App.config.ServerEngineConfig;
import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.tasks.ClientPushTask;
import App.service.tasks.ProcessFlusherTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        if(this.processState == ProcessState.CLOSED) {
            return;
        }

        this.processState = ProcessState.CLOSING;
        log.info("Unloading process");
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

    public void closeProcessThreads() {
        try {
            this.threadSignallingConfiguration.setShutdown(true);
            Thread.sleep(200);
            if (processFlusherService != null && clientPushServiceExecutor != null) {
                this.processFlusherService.shutdownNow();
                this.clientPushServiceExecutor.shutdownNow();
            }
        } catch (Exception e) {
            log.error("Exception in closing process threads");
        }
    }

    public synchronized void reload() {
        try {
            heartBeatService.processHeartBeat();
            log.info("Reloading");
            if(process != null) {
                unloadProcess();
            }
            this.processState = ProcessState.STARTED;
            this.processFlusherService = Executors.newFixedThreadPool(1);
            this.clientPushServiceExecutor = Executors.newFixedThreadPool(1);
            threadSignallingConfiguration.setShutdown(false);
            Runtime rt = Runtime.getRuntime();
            String command = serverEngineConfig.getEngineName();
            this.process = rt.exec(command);

            ClientPushTask clientPushTask = new ClientPushTask(process, pendingMessagePushService, threadSignallingConfiguration, new ArrayDeque<>());
            clientPushServiceExecutor.submit(clientPushTask);
            Thread.sleep(100);

            ProcessFlusherTask processFlusherTask = new ProcessFlusherTask(process, serverQueueConfig, threadSignallingConfiguration, this);
            processFlusherService.submit(processFlusherTask);


            log.info("Loaded");
            //serverQueueConfig.offer("readyok");
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
}
