package com.ironcompass.ui;

import com.ironcompass.planner.ProgressionCandidate;
import com.ironcompass.planner.RecommendationProjection;
import java.awt.Component;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

import static com.ironcompass.ui.UiComponents.*;

/** Renders the account-aware "Suggested for You" cards. */
final class SuggestedRenderer
{
    interface CandidateActionFactory
    {
        JButton create(ProgressionCandidate candidate);
    }

    void addRecommendations(JPanel home, RecommendationProjection recommendations, boolean primaryAlreadyShown,
                            boolean showUsefulBreaks, Runnable toggleUsefulBreaks,
                            CandidateActionFactory actionFactory)
    {
        if (recommendations.getRecommended() != null && !primaryAlreadyShown)
        {
            home.add(buildCandidate("RECOMMENDED", recommendations.getRecommended(), actionFactory));
        }
        if (recommendations.getQuickWin() != null)
        {
            if (recommendations.getRecommended() != null && !primaryAlreadyShown) home.add(gap(10));
            home.add(buildCandidate("QUICK WIN", recommendations.getQuickWin(), actionFactory));
        }
        if (recommendations.getLongTerm() != null)
        {
            if ((recommendations.getRecommended() != null && !primaryAlreadyShown)
                || recommendations.getQuickWin() != null) home.add(gap(10));
            home.add(buildCandidate("LONG-TERM", recommendations.getLongTerm(), actionFactory));
        }
        if (!recommendations.getUsefulBreaks().isEmpty())
        {
            home.add(gap(8));
            JButton alternatives = ghostButton(showUsefulBreaks ? "HIDE OTHER PROGRESS" : "TAKE A USEFUL BREAK");
            alternatives.setAlignmentX(Component.LEFT_ALIGNMENT);
            alternatives.setToolTipText("Show other actions that still advance this account");
            alternatives.addActionListener(event -> toggleUsefulBreaks.run());
            home.add(alternatives);
            if (showUsefulBreaks)
            {
                home.add(gap(7));
                home.add(buildUsefulBreaks(recommendations.getUsefulBreaks()));
            }
        }
    }

    private JPanel buildCandidate(String label, ProgressionCandidate candidate,
                                  CandidateActionFactory actionFactory)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(label));
        body.add(gap(4));
        body.add(labelHtml("<b>" + escape(candidate.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(humanize(candidate.getImpact()) + " - "
            + humanize(candidate.getEffort().name()) + " effort", UiTokens.MUTED));
        body.add(gap(5));
        body.add(sectionLabel("WHY THIS?"));
        body.add(gap(2));
        int reasonCount = 0;
        for (String why : candidate.getWhyLines())
        {
            body.add(labelHtml("-  " + escape(why), UiTokens.TEXT_SECONDARY));
            if (++reasonCount == 2) break;
        }
        if (candidate.getActiveGoalCount() > 1)
            body.add(labelHtml("Goals: " + escape(String.join(", ", candidate.getAdvancedGoals())),
                UiTokens.MUTED));
        else if (candidate.getUnlockSummary() != null && !candidate.getUnlockSummary().equals(candidate.getReason()))
        {
            body.add(labelHtml("Unlocks: " + escape(candidate.getUnlockSummary()), UiTokens.MUTED));
        }
        JButton open = actionFactory.create(candidate);
        if (open != null)
        {
            body.add(gap(7));
            body.add(open);
        }
        CardStyle style = "RECOMMENDED".equals(label) ? CardStyle.HERO
            : "QUICK WIN".equals(label) ? CardStyle.SUCCESS : CardStyle.SUBTLE;
        return card(body, style);
    }

    private JPanel buildUsefulBreaks(List<ProgressionCandidate> alternatives)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("OTHER USEFUL PROGRESS"));
        int shown = 0;
        for (ProgressionCandidate candidate : alternatives)
        {
            if (shown++ > 0) body.add(gap(5));
            body.add(labelHtml("<b>" + shown + ". " + escape(candidate.getTitle()) + "</b>", UiTokens.TEXT));
            String why = candidate.getWhyLines().isEmpty() ? candidate.getReason() : candidate.getWhyLines().get(0);
            body.add(labelHtml(escape(why), UiTokens.MUTED));
            if (shown == 3) break;
        }
        return card(body, CardStyle.SUBTLE);
    }

    private static String humanize(String value)
    {
        String lower = value.toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;").replace("\n", "<br>");
    }
}
