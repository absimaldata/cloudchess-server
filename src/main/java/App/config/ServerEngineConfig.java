package App.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class ServerEngineConfig {
    private String engineName;
}
