package App.controller;

import App.service.ProcessManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ReloadController {
    @Autowired
    private ProcessManagerService processManagerService;

    @GetMapping("/reload")
    public void reload() {
        log.info("Client just started, so reloading");
        processManagerService.reload();
    }

    @GetMapping("/unload")
    public void unload() {
        log.info("Got quit line");
        processManagerService.unloadProcess();
    }
}
