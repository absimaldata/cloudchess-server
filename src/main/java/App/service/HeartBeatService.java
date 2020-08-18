package App.service;

import App.enums.ProcessState;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class HeartBeatService {

    @Autowired
    private ProcessManagerService processManagerService;

    private long lastUpdatedTime = System.currentTimeMillis();

    public void processHeartBeat() {
        lastUpdatedTime = System.currentTimeMillis();
    }

    //@Scheduled(fixedRate = 500)
    public void checkHeartBeat() {
        if((System.currentTimeMillis() - lastUpdatedTime) > 60000) {
            if(processManagerService.getProcessState() == ProcessState.RUNNING) {
                log.info("Got no heartbeat so unloading");
                processManagerService.unloadProcess();
            }
        }
    }
}
