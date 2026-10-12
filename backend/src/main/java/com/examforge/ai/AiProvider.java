package com.examforge.ai;

public interface AiProvider {
    String getProviderName();
    boolean isAvailable();
    AiModels.GenerationResult generateQuestions(AiModels.QuestionGenerationRequest request);
    AiModels.GeneratedQuestion regenerateQuestion(AiModels.QuestionGenerationRequest request, AiModels.GeneratedQuestion existingQuestion, String instructions);
}
