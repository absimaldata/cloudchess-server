package App.service;

import App.config.ServerEngineConfig;
import App.dto.ConfigFileDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;

@Service
@Log4j2
public class ConfigFileAutoLoader {

    @Value("${config.file}")
    private String configFile;

    @Autowired
    private ServerEngineConfig serverEngineConfig;

    private static long lastModified = 0;

    @PostConstruct
    public void init() {
        doWork();
    }

    @Scheduled(fixedRate = 1000)
    public void doWork() {
        try {
            File file = new File(configFile);
            long mod = file.lastModified();
            if (mod != lastModified) {
                log.info("Loading a new engine");
                lastModified = mod;
                String txt = IOUtils.toString(new FileReader(file));
                ObjectMapper objectMapper = new ObjectMapper();
                ConfigFileDto configFileDto = objectMapper.readValue(txt, ConfigFileDto.class);
                serverEngineConfig.setEngineName(configFileDto.getEngineName());
                log.info("Loaded Engine Name: " + serverEngineConfig.getEngineName());
            }
        } catch (Exception e) {
            log.error("Error loading engine", e);
            lastModified = 0;
        }
    }
}
