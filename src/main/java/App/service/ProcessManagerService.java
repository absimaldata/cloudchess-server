package App.service;

import App.config.ServerEngineConfig;
import App.config.ServerQueueConfig;
import App.config.ThreadSignallingConfiguration;
import App.enums.ProcessState;
import App.service.tasks.ClientPushTask;
import App.service.tasks.ProcessFlusherTask;
import App.service.tasks.StdErrReaderTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
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

    private ExecutorService threadExecutor = Executors.newFixedThreadPool(3);

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
            this.threadSignallingConfiguration.setProcessFlusherTask(true);
            this.threadSignallingConfiguration.setClientPushTask(true);
            while(this.threadSignallingConfiguration.isClientPushTask() || this.threadSignallingConfiguration.isProcessFlusherTask()) {
                log.info("Waiting for threads to close");
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log.error("Exception in closing process threads");
        }
    }

    public synchronized void reload() {
        try {
            if(this.processState == ProcessState.STARTED || this.processState == ProcessState.CLOSING) {
                return;
            }

            this.processState = ProcessState.CLOSING;
            log.info("Reloading");
            if(process != null) {
                unloadProcess();
            }

            this.threadExecutor = Executors.newFixedThreadPool(3);

            Runtime rt = Runtime.getRuntime();
            String command = serverEngineConfig.getEngineName();
            this.process = rt.exec(command);

            ClientPushTask clientPushTask = new ClientPushTask(process, pendingMessagePushService, threadSignallingConfiguration, new ArrayDeque<>());
            this.threadExecutor.submit(clientPushTask);

            log.info("Submitted client push task");

            BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            ProcessFlusherTask processFlusherTask = new ProcessFlusherTask(processWriter, serverQueueConfig, threadSignallingConfiguration, this, serverEngineConfig.getEngineOptions());
            this.threadExecutor.submit(processFlusherTask);

            log.info("Submitted process flusher task");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StdErrReaderTask stdErrReaderTask = new StdErrReaderTask(bufferedReader, pendingMessagePushService);
            this.threadExecutor.submit(stdErrReaderTask);

            log.info("Submitted error reader task");

            Thread.sleep(200);
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
        synchronized (this.processState) {
            this.processState = processState;
        }
    }

    public String isProcessStarted() {
        return this.processState.name();
    }
}
