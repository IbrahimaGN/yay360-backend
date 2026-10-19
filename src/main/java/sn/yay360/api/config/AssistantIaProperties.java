package sn.yay360.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "yay360.assistant")
public class AssistantIaProperties {

    private boolean enabled;
    private String endpoint;
    private String apiKey;
    private String model;
    private String transcriptionEndpoint;
    private String transcriptionModel;
    private String transcriptionLanguage = "fr";
    private String speechEndpoint;
    private String speechModel;
    private String speechVoice = "alloy";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private long maxAudioBytes = 10 * 1024 * 1024;
}
