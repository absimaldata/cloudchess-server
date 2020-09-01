package App.config;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

@Component
@Data
@Log4j2
public class ServerEngineConfig {
    private String engineName;
    private Properties engineOptions;

    @Value("${engine.config.file}")
    private String configFile;

    @PostConstruct
    public void init() {
        try {
            InputStream inputStream = new FileInputStream(configFile);
            engineOptions = new Properties();
            engineOptions.load(inputStream);
            log.info("Properties loaded:" + engineOptions.toString());
        } catch (Exception e) {
            log.error("Error loading properties", e);
        }
    }
}
