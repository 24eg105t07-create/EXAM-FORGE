package com.examforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "examforge")
public class ApplicationProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Ai ai = new Ai();
    private Bootstrap bootstrap = new Bootstrap();
    private College college = new College();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "ExamForgeSecretKeyMustBeAtLeast256BitsLongForHMACSHA256Signature2026Secure!";
        private long expirationMs = 86400000L; // 24 hours
        private long refreshExpirationMs = 604800000L; // 7 days
    }

    @Getter
    @Setter
    public static class Upload {
        private String directory = "./uploads";
        private int maxFileSizeMb = 15;
    }

    @Getter
    @Setter
    public static class Ai {
        private String provider = "LOCAL_DEMO"; // LOCAL_DEMO, GEMINI, OPENAI
        private ProviderConfig gemini = new ProviderConfig("gemini-1.5-flash");
        private ProviderConfig openai = new ProviderConfig("gpt-4o-mini");
    }

    @Getter
    @Setter
    public static class ProviderConfig {
        private String apiKey = "";
        private String model = "";

        public ProviderConfig() {}

        public ProviderConfig(String model) {
            this.model = model;
        }
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminEmail = "admin@college.edu";
        private String adminPassword = "Admin@ExamForge2026";
        private String adminName = "College SuperAdmin";
    }

    @Getter
    @Setter
    public static class College {
        private String timezone = "Asia/Kolkata";
    }
}
