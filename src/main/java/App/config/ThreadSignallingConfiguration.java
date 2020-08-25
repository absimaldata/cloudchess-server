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
    private boolean stopRead = false;
    private boolean stopState = false;

    public boolean isProcessFlusherTask() {
        return processFlusherTask;
    }

    public synchronized void setProcessFlusherTask(boolean processFlusherTask) {
        this.processFlusherTask = processFlusherTask;
    }

    public boolean isClientPushTask() {
        return clientPushTask;
    }

    public synchronized void setClientPushTask(boolean clientPushTask) {
        this.clientPushTask = clientPushTask;
    }

    public boolean isStopAnalysis() {
        return stopAnalysis;
    }

    public void setStopAnalysis(boolean stopAnalysis) {
        this.stopAnalysis = stopAnalysis;
    }

    public void setStopRead(boolean value) {
        this.stopRead = value;
    }

    public boolean isStopRead() {
        return this.stopRead;
    }

    public boolean isStopState() {
        return stopState;
    }

    public void setStopState(boolean stopState) {
        this.stopState = stopState;
    }
}
