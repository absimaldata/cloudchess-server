package App.config;

import org.springframework.stereotype.Component;

@Component
public class ThreadSignallingConfiguration {

    /**
     * For clean shutdown
     */

    private boolean stopAnalysis = false;
    private boolean clientPushTask = false;
    private boolean processFlusherTask = false;

    public boolean isProcessFlusherTask() {
        return processFlusherTask;
    }

    public void setProcessFlusherTask(boolean processFlusherTask) {
        this.processFlusherTask = processFlusherTask;
    }

    public boolean isClientPushTask() {
        return clientPushTask;
    }

    public void setClientPushTask(boolean clientPushTask) {
        this.clientPushTask = clientPushTask;
    }

    public boolean isStopAnalysis() {
        return stopAnalysis;
    }

    public void setStopAnalysis(boolean stopAnalysis) {
        this.stopAnalysis = stopAnalysis;
    }
}
