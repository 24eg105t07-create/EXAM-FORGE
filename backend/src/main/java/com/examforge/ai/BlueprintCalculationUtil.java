package com.examforge.ai;

import com.examforge.exception.BadRequestException;

import java.util.*;

public final class BlueprintCalculationUtil {

    private BlueprintCalculationUtil() {}

    public static DifficultyDistribution calculateDifficultyDistribution(int totalQuestions, int easyPct, int mediumPct, int hardPct) {
        if (easyPct + mediumPct + hardPct != 100) {
            throw new BadRequestException(String.format(
                    "Difficulty percentages must sum to exactly 100%%. Provided: Easy %d%% + Medium %d%% + Hard %d%% = %d%%",
                    easyPct, mediumPct, hardPct, easyPct + mediumPct + hardPct));
        }

        if (totalQuestions <= 0) {
            throw new BadRequestException("Total questions must be greater than zero");
        }

        // Deterministic largest remainder method / round strategy
        int easyCount = (int) Math.round((double) totalQuestions * easyPct / 100.0);
        int mediumCount = (int) Math.round((double) totalQuestions * mediumPct / 100.0);
        int hardCount = totalQuestions - easyCount - mediumCount;

        // If hardCount became negative or unbalanced due to rounding, adjust deterministically
        if (hardCount < 0) {
            int deficit = -hardCount;
            if (mediumCount >= deficit) {
                mediumCount -= deficit;
            } else {
                easyCount -= (deficit - mediumCount);
                mediumCount = 0;
            }
            hardCount = 0;
        }

        return new DifficultyDistribution(easyCount, mediumCount, hardCount);
    }

    public static Map<Long, Integer> calculateLessonDistribution(int totalQuestions, Map<Long, Integer> lessonPercentages) {
        int sumPct = lessonPercentages.values().stream().mapToInt(Integer::intValue).sum();
        if (sumPct != 100) {
            throw new BadRequestException("Lesson distribution percentages must sum to 100%. Provided sum: " + sumPct + "%");
        }

        Map<Long, Integer> result = new LinkedHashMap<>();
        int allocated = 0;

        List<Map.Entry<Long, Integer>> sortedEntries = new ArrayList<>(lessonPercentages.entrySet());
        // Sort descending by percentage
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<Long, Integer> entry = sortedEntries.get(i);
            if (i == sortedEntries.size() - 1) {
                // Last lesson receives remaining questions
                result.put(entry.getKey(), Math.max(0, totalQuestions - allocated));
            } else {
                int count = (int) Math.round((double) totalQuestions * entry.getValue() / 100.0);
                result.put(entry.getKey(), count);
                allocated += count;
            }
        }

        return result;
    }

    public record DifficultyDistribution(int easyCount, int mediumCount, int hardCount) {}
}
