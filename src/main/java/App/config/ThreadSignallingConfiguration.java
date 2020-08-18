package App.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
public class ThreadSignallingConfiguration {

    /**
     * For clean shutdown
     */

    private boolean shutdown = false;
    private boolean stopAnalysis = false;

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public boolean isStopAnalysis() {
        return stopAnalysis;
    }

    public void setStopAnalysis(boolean stopAnalysis) {
        this.stopAnalysis = stopAnalysis;
    }
}
