package App.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
public class ServerEngineConfig {
    private String engineName;
    private Map<String, String> optionNameValueMap = new HashMap<>();
}
