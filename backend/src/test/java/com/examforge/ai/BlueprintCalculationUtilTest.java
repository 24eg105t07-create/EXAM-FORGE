package com.examforge.ai;

import com.examforge.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintCalculationUtilTest {

    @Test
    @DisplayName("Should correctly calculate 30% easy, 50% medium, 20% hard for 20 questions")
    void testStandardDifficultyDistribution() {
        BlueprintCalculationUtil.DifficultyDistribution dist =
                BlueprintCalculationUtil.calculateDifficultyDistribution(20, 30, 50, 20);

        assertEquals(6, dist.easyCount(), "30% of 20 must be 6");
        assertEquals(10, dist.mediumCount(), "50% of 20 must be 10");
        assertEquals(4, dist.hardCount(), "20% of 20 must be 4");
        assertEquals(20, dist.easyCount() + dist.mediumCount() + dist.hardCount(), "Total must sum to 20");
    }

    @Test
    @DisplayName("Should reject distributions that do not sum to 100%")
    void testInvalidPercentageSum() {
        assertThrows(BadRequestException.class, () ->
                BlueprintCalculationUtil.calculateDifficultyDistribution(20, 30, 40, 20)); // sums to 90%
    }

    @Test
    @DisplayName("Should handle odd question counts with deterministic remainder")
    void testOddQuestionCountDistribution() {
        BlueprintCalculationUtil.DifficultyDistribution dist =
                BlueprintCalculationUtil.calculateDifficultyDistribution(15, 33, 34, 33);

        assertEquals(15, dist.easyCount() + dist.mediumCount() + dist.hardCount());
        assertTrue(dist.easyCount() >= 4);
        assertTrue(dist.mediumCount() >= 4);
    }

    @Test
    @DisplayName("Should calculate lesson distribution percentages summing to 100%")
    void testLessonDistribution() {
        Map<Long, Integer> lessonPcts = Map.of(1L, 50, 2L, 50);
        Map<Long, Integer> distribution = BlueprintCalculationUtil.calculateLessonDistribution(20, lessonPcts);

        assertEquals(10, distribution.get(1L));
        assertEquals(10, distribution.get(2L));
        assertEquals(20, distribution.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    @DisplayName("Should reject lesson distribution that does not sum to 100%")
    void testInvalidLessonDistribution() {
        Map<Long, Integer> lessonPcts = Map.of(1L, 40, 2L, 40);
        assertThrows(BadRequestException.class, () ->
                BlueprintCalculationUtil.calculateLessonDistribution(20, lessonPcts));
    }
}
