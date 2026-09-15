package com.example.heritagetrace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftMatcherTest {

    @Test
    void sameCraftMatches() {
        assertTrue(CraftMatcher.isCraftMatched("木雕", "木雕"));
    }

    @Test
    void differentCraftDoesNotMatch() {
        // 木雕凿子不能挂到刺绣项目名下
        assertFalse(CraftMatcher.isCraftMatched("木雕", "刺绣"));
    }

    @Test
    void blankSideIsToleratedForLegacyRecords() {
        assertTrue(CraftMatcher.isCraftMatched(null, "刺绣"));
        assertTrue(CraftMatcher.isCraftMatched("木雕", ""));
        assertTrue(CraftMatcher.isCraftMatched("  ", null));
    }

    @Test
    void matchIgnoresSurroundingWhitespaceAndCase() {
        assertTrue(CraftMatcher.isCraftMatched("  木雕 ", "木雕"));
        assertTrue(CraftMatcher.isCraftMatched("Woodcarving", "woodcarving"));
    }

    @Test
    void mismatchMessageNamesBothCrafts() {
        String message = CraftMatcher.mismatchMessage("木雕", "刺绣");
        assertTrue(message.contains("木雕"));
        assertTrue(message.contains("刺绣"));
        assertTrue(message.contains("工艺对不上"));
    }
}
