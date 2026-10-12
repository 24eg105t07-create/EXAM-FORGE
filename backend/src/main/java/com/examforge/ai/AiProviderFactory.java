package com.examforge.ai;

import com.examforge.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private final ApplicationProperties properties;
    private final LocalDemoAiProvider localDemoAiProvider;
    private final GeminiAiProvider geminiAiProvider;
    private final OpenAiProvider openAiProvider;

    public AiProvider getProvider() {
        String configuredProvider = properties.getAi().getProvider();
        if ("GEMINI".equalsIgnoreCase(configuredProvider)) {
            return geminiAiProvider;
        } else if ("OPENAI".equalsIgnoreCase(configuredProvider)) {
            return openAiProvider;
        }
        return localDemoAiProvider;
    }
}
