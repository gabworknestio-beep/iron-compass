package com.ironcompass.ui;

import com.ironcompass.planner.AccountNeedEvaluation;
import com.ironcompass.planner.AccountNeedLevel;
import com.ironcompass.planner.GoalBlocker;
import com.ironcompass.planner.GoalInsightsProjection;
import com.ironcompass.planner.GoalPackProjection;
import com.ironcompass.planner.GoalProximityCandidate;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.ironcompass.ui.UiComponents.*;

/** Compact first-screen account diagnosis for the RuneLite sidebar. */
final class AccountInsightSummaryRenderer
{
    JPanel render(GoalInsightsProjection insights, boolean bankObserved, Runnable openDetails)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("ACCOUNT OVERVIEW"));
        body.add(gap(UiTokens.SM));
        for (AccountNeedEvaluation value : orderedHealth(insights))
        {
            body.add(healthRow(value));
        }
        if (!bankObserved)
        {
            body.add(gap(UiTokens.SM));
            body.add(labelHtml("<b>BANK UNKNOWN</b> - reserves are not treated as empty", UiTokens.WARNING));
        }
        addProximity(body, "QUICK WINS", insights.getQuickWins(), UiTokens.SUCCESS);
        addProximity(body, "UNLOCK SOON", insights.getNearbyUnlocks(), UiTokens.ACCENT);
        addBlockers(body, insights.getBlockers());
        addPacks(body, insights.getGoalPacks());

        JButton details = smallButton("VIEW ACCOUNT INSIGHTS");
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.addActionListener(event -> openDetails.run());
        body.add(gap(UiTokens.SM));
        body.add(details);
        return card(body, CardStyle.SUBTLE);
    }

    private static List<AccountNeedEvaluation> orderedHealth(GoalInsightsProjection insights)
    {
        List<AccountNeedEvaluation> health = new ArrayList<>(insights.getHealth().getEvaluations());
        health.sort(Comparator.comparingInt(AccountInsightSummaryRenderer::healthSummaryOrder));
        return health.subList(0, Math.min(4, health.size()));
    }

    private static JPanel healthRow(AccountNeedEvaluation evaluation)
    {
        JPanel row = new JPanel(new BorderLayout(UiTokens.SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel("-  " + compactHealthLabel(evaluation));
        name.setForeground(UiTokens.TEXT_SECONDARY);
        name.setFont(UiTokens.BODY);
        JLabel status = new JLabel(humanize(evaluation.getLevel().name()));
        status.setForeground(healthColor(evaluation.getLevel()));
        status.setFont(UiTokens.META.deriveFont(java.awt.Font.BOLD));
        row.add(name, BorderLayout.CENTER);
        row.add(status, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 2));
        row.getAccessibleContext().setAccessibleName(name.getText() + " " + status.getText());
        return row;
    }

    private static void addProximity(JPanel body, String title, List<GoalProximityCandidate> candidates,
                                     Color color)
    {
        if (candidates.isEmpty()) return;
        body.add(gap(UiTokens.MD));
        body.add(sectionLabel(title));
        int shown = 0;
        for (GoalProximityCandidate candidate : candidates)
        {
            body.add(labelHtml("<b>" + escape(candidate.getGoal().getTitle()) + "</b>", color));
            body.add(labelHtml(escape(candidate.getSummary()), UiTokens.TEXT_MUTED));
            if (++shown == 2) break;
        }
    }

    private static void addBlockers(JPanel body, List<GoalBlocker> blockers)
    {
        if (blockers.isEmpty()) return;
        body.add(gap(UiTokens.MD));
        body.add(sectionLabel("CURRENT BLOCKERS"));
        int shown = 0;
        for (GoalBlocker blocker : blockers)
        {
            body.add(labelHtml("<b>" + escape(blocker.getTitle()) + "</b>", UiTokens.WARNING));
            body.add(labelHtml(escape(blocker.getExplanation()), UiTokens.TEXT_MUTED));
            if (++shown == 2) break;
        }
    }

    private static void addPacks(JPanel body, List<GoalPackProjection> packs)
    {
        if (packs.isEmpty()) return;
        body.add(gap(UiTokens.MD));
        body.add(sectionLabel("GOAL PACKS"));
        int shown = 0;
        for (GoalPackProjection pack : packs)
        {
            body.add(labelHtml("<b>" + escape(pack.getTitle()) + "</b> - "
                + pack.getCompleteCount() + "/" + pack.getTotalCount(), packColor(pack.getStatus())));
            body.add(labelHtml(escape(pack.getSummary()), UiTokens.TEXT_MUTED));
            if (++shown == 2) break;
        }
    }

    private static int healthSummaryOrder(AccountNeedEvaluation evaluation)
    {
        switch (evaluation.getIntent())
        {
            case BOSSING_READINESS: return 0;
            case MELEE_POWER: return 1;
            case RANGED_POWER: return 2;
            case MAGIC_POWER: return 3;
            default: return 10 + healthRank(evaluation.getLevel());
        }
    }

    private static int healthRank(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.WEAK) return 0;
        if (level == AccountNeedLevel.DEVELOPING) return 1;
        if (level == AccountNeedLevel.UNKNOWN) return 2;
        if (level == AccountNeedLevel.GOOD) return 3;
        return 4;
    }

    private static String compactHealthLabel(AccountNeedEvaluation evaluation)
    {
        switch (evaluation.getIntent())
        {
            case BOSSING_READINESS: return "Bossing";
            case FOOD_SUSTAIN: return "Food";
            case MAGIC_POWER: return "Magic";
            case MELEE_POWER: return "Melee";
            case POH_NETWORK: return "POH";
            case PRAYER_SUSTAIN: return "Prayer";
            case RANGED_POWER: return "Ranged";
            case RUNE_SUPPLY: return "Runes";
            case TRANSPORT_NETWORK: return "Transport";
            default: return humanize(evaluation.getIntent().name());
        }
    }

    private static Color healthColor(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.STRONG) return UiTokens.SUCCESS.brighter();
        if (level == AccountNeedLevel.GOOD) return UiTokens.SUCCESS;
        if (level == AccountNeedLevel.DEVELOPING) return UiTokens.WARNING;
        if (level == AccountNeedLevel.WEAK) return new Color(178, 126, 91);
        return UiTokens.TEXT_MUTED;
    }

    private static Color packColor(GoalPackProjection.Status status)
    {
        switch (status)
        {
            case READY: return UiTokens.SUCCESS;
            case CLOSE: return UiTokens.ACCENT;
            case BUILDING: return UiTokens.WARNING;
            default: return UiTokens.UNKNOWN;
        }
    }

    private static String humanize(String value)
    {
        String lower = value.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;").replace("\n", "<br>");
    }
}
