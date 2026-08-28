package com.ironcompass;

import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.Locale;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class AccountStateAssemblyPerformanceTest
{
    @Test
    public void fullSkillAndQuestSnapshotAssemblyStaysLightweight()
    {
        int iterations = 500;
        long total = 0L;
        long questTotal = 0L;
        for (int iteration = 0; iteration < iterations; iteration++)
        {
            long start = System.nanoTime();
            AccountState.Builder builder = AccountState.builder().accountMode(AccountMode.IRONMAN)
                .bank(BankSnapshot.observed(Collections.emptyMap(), 1L));
            for (Skill skill : Skill.values()) builder.skill(skill.getName(), 70);
            long questStart = System.nanoTime();
            for (Quest quest : Quest.values()) builder.quest(quest.getName(), QuestProgress.NOT_STARTED);
            questTotal += System.nanoTime() - questStart;
            builder.build();
            total += System.nanoTime() - start;
        }
        double averageMs = total / 1_000_000.0 / iterations;
        double questAverageMs = questTotal / 1_000_000.0 / iterations;
        System.out.printf(Locale.ENGLISH,
            "Iron Compass skill + full quest snapshot assembly: avg %.3f ms; quest copy %.3f ms; %d iterations%n",
            averageMs, questAverageMs, iterations);
        assertTrue("Snapshot assembly should stay under 10 ms average, was " + averageMs, averageMs < 10.0);
        assertTrue("Quest copy should stay under 5 ms average, was " + questAverageMs, questAverageMs < 5.0);
    }
}
