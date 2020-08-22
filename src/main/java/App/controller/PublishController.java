package App.controller;

import App.config.ServerQueueConfig;
import App.constants.ResponseStatus;
import App.service.HeartBeatService;
import App.service.PendingMessagePushService;
import App.service.ProcessManagerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Log4j2
public class PublishController {

    @Autowired
    private ServerQueueConfig serverQueueConfig;

    @Autowired
    private ProcessManagerService processManagerService;

    @Autowired
    private HeartBeatService heartBeatService;

    @Autowired
    private PendingMessagePushService pendingMessagePushService;

    @PostMapping("/publish")
    public String publish(@RequestBody String line) {
        try {
            log.info("Recieved line: " + line);
            serverQueueConfig.offer(line);
            return ResponseStatus.SUCCESS;
        } catch (Exception e) {
            log.error("Error occurred", e);
            return ResponseStatus.ERROR;
        }
    }

    @GetMapping("/pull")
    public List<String> pull() {
        List<String> msg =  pendingMessagePushService.pollMessages();
        return msg;
    }

    @GetMapping("/heartbeat")
    public void heartbeat() {
        heartBeatService.processHeartBeat();
    }

    @GetMapping("/checkProcess")
    public String checkProcess() {
        return processManagerService.isProcessStarted();
    }

}
