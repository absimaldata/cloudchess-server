package App.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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

}
