package com.ironcompass.ui;

import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.planner.AccountNeedEvaluation;
import com.ironcompass.planner.AccountNeedLevel;
import com.ironcompass.planner.GoalBlocker;
import com.ironcompass.planner.GoalInsightsProjection;
import com.ironcompass.planner.GoalPackProjection;
import com.ironcompass.planner.GoalPathNode;
import com.ironcompass.planner.GoalProximityCandidate;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import static com.ironcompass.ui.UiComponents.*;

/** Read-only, themed account diagnosis assembled from the existing insight projection. */
final class AccountInsightsDialog
{
    private final Component owner;
    private final GoalInsightsProjection insights;
    private final JPanel root = new JPanel(new BorderLayout());

    AccountInsightsDialog(Component owner, GoalInsightsProjection insights)
    {
        this.owner = owner;
        this.insights = insights;
        build();
    }

    void showDialog()
    {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        Window window = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, "Iron Compass · Account Insights",
            Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(400, 560));
        dialog.setPreferredSize(new Dimension(430, 650));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    JPanel contentForTesting()
    {
        return root;
    }

    private void build()
    {
        root.setBackground(UiTokens.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(UiTokens.LG, UiTokens.LG, UiTokens.LG, UiTokens.LG));
        root.getAccessibleContext().setAccessibleName("Iron Compass Account Insights");

        JPanel content = scrollableVerticalPanel();
        content.add(header());
        content.add(gap(UiTokens.LG));
        content.add(healthCard());
        content.add(gap(UiTokens.MD));
        content.add(proximityCard());
        content.add(gap(UiTokens.MD));
        content.add(goalPackCard());
        content.add(gap(UiTokens.MD));
        content.add(blockerCard());
        content.add(gap(UiTokens.MD));
        content.add(alternativesCard());
        content.add(gap(UiTokens.MD));
        content.add(personalPathCard());
        content.add(gap(UiTokens.LG));

        JScrollPane scroll = scrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, UiTokens.MD));
        footer.setOpaque(false);
        JButton close = primaryButton("DONE");
        close.getAccessibleContext().setAccessibleDescription("Close Account Insights");
        close.addActionListener(event ->
        {
            Window window = SwingUtilities.getWindowAncestor(root);
            if (window != null) window.dispose();
        });
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);
    }

    private JPanel header()
    {
        JPanel header = new JPanel(new BorderLayout(UiTokens.MD, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (compassIcon() != null) header.add(new JLabel(compassIcon()), BorderLayout.WEST);
        JPanel copy = verticalPanel();
        copy.add(cardTitle("ACCOUNT INSIGHTS", UiTokens.ACCENT));
        copy.add(labelHtml("A focused diagnosis of the progression data Iron Compass already knows.",
            UiTokens.TEXT_MUTED));
        header.add(copy, BorderLayout.CENTER);
        return header;
    }

    private JPanel healthCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("ACCOUNT HEALTH"));
        body.add(gap(UiTokens.SM));
        for (AccountNeedEvaluation evaluation : insights.getHealth().getEvaluations())
        {
            body.add(healthRow(evaluation));
            body.add(gap(UiTokens.SM));
        }
        return card(body, CardStyle.HERO);
    }

    private JPanel healthRow(AccountNeedEvaluation evaluation)
    {
        JPanel row = new JPanel(new BorderLayout(UiTokens.MD, 0));
        row.setOpaque(false);
        JPanel copy = verticalPanel();
        copy.add(cardTitle(humanize(evaluation.getIntent().name()), UiTokens.TEXT_PRIMARY));
        copy.add(labelHtml(escape(evaluation.getPrimaryExplanation()), UiTokens.TEXT_MUTED));
        row.add(copy, BorderLayout.CENTER);
        JLabel state = badge(humanize(evaluation.getLevel().name()), healthColor(evaluation.getLevel()));
        state.setToolTipText(evaluation.getPrimaryExplanation());
        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        status.setOpaque(false);
        status.add(state);
        row.add(status, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.getAccessibleContext().setAccessibleName(humanize(evaluation.getIntent().name()) + " "
            + humanize(evaluation.getLevel().name()));
        return row;
    }

    private JPanel proximityCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("QUICK WINS & UNLOCK RADAR"));
        body.add(gap(UiTokens.SM));
        addCandidates(body, "QUICK WIN", insights.getQuickWins(), UiTokens.SUCCESS,
            "No reliable quick win in the current snapshot.");
        body.add(gap(UiTokens.MD));
        addCandidates(body, "UNLOCK SOON", insights.getNearbyUnlocks(), UiTokens.ACCENT,
            "No nearby unlock in the current snapshot.");
        return card(body, CardStyle.SUCCESS);
    }

    private void addCandidates(JPanel body, String role, List<GoalProximityCandidate> candidates,
                               Color accent, String empty)
    {
        if (candidates.isEmpty())
        {
            body.add(labelHtml(empty, UiTokens.TEXT_MUTED));
            return;
        }
        int limit = Math.min(3, candidates.size());
        for (int index = 0; index < limit; index++)
        {
            GoalProximityCandidate candidate = candidates.get(index);
            JPanel row = verticalPanel();
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 2, 0, 0, accent.darker()),
                BorderFactory.createEmptyBorder(UiTokens.XS, UiTokens.MD, UiTokens.XS, 0)));
            JPanel title = new JPanel(new BorderLayout(UiTokens.SM, 0));
            title.setOpaque(false);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.add(cardTitle(candidate.getGoal().getTitle(), UiTokens.TEXT_PRIMARY), BorderLayout.CENTER);
            title.add(badge(candidate.isKnown() ? role : "UNKNOWN", candidate.isKnown() ? accent : UiTokens.UNKNOWN),
                BorderLayout.EAST);
            row.add(title);
            row.add(labelHtml(escape(candidate.getSummary()), UiTokens.TEXT_MUTED));
            body.add(row);
            if (index + 1 < limit) body.add(gap(UiTokens.SM));
        }
    }

    private JPanel blockerCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("PRIMARY BLOCKERS"));
        body.add(gap(UiTokens.SM));
        if (insights.getBlockers().isEmpty())
        {
            body.add(labelHtml("No primary-goal blocker is currently selected.", UiTokens.TEXT_MUTED));
            return card(body, CardStyle.SUBTLE);
        }
        for (GoalBlocker blocker : insights.getBlockers())
        {
            JPanel title = new JPanel(new BorderLayout(UiTokens.SM, 0));
            title.setOpaque(false);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.add(cardTitle(blocker.getTitle(), UiTokens.TEXT_PRIMARY), BorderLayout.CENTER);
            title.add(badge(humanize(blocker.getKind().name()), blockerColor(blocker)), BorderLayout.EAST);
            body.add(title);
            body.add(labelHtml(escape(blocker.getExplanation()), UiTokens.TEXT_MUTED));
            body.add(gap(UiTokens.MD));
        }
        return card(body, CardStyle.WARNING);
    }

    private JPanel goalPackCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("GOAL PACKS"));
        body.add(gap(UiTokens.SM));
        if (insights.getGoalPacks().isEmpty())
        {
            body.add(labelHtml("No active pack is currently ranked.", UiTokens.TEXT_MUTED));
            return card(body, CardStyle.SUBTLE);
        }
        for (GoalPackProjection pack : insights.getGoalPacks())
        {
            JPanel title = new JPanel(new BorderLayout(UiTokens.SM, 0));
            title.setOpaque(false);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.add(cardTitle(pack.getTitle(), UiTokens.TEXT_PRIMARY), BorderLayout.CENTER);
            title.add(badge(pack.getCompleteCount() + "/" + pack.getTotalCount(), packColor(pack.getStatus())),
                BorderLayout.EAST);
            body.add(title);
            body.add(labelHtml(escape(pack.getSummary()), UiTokens.TEXT_MUTED));
            int shown = 0;
            for (GoalBlocker blocker : pack.getBlockers())
            {
                body.add(statusLine(blocker.getKind() == GoalBlocker.Kind.HARD_REQUIREMENT
                    ? com.ironcompass.requirement.TruthValue.FALSE
                    : com.ironcompass.requirement.TruthValue.UNKNOWN,
                    blocker.getTitle() + ": " + blocker.getExplanation()));
                if (++shown == 2) break;
            }
            body.add(gap(UiTokens.MD));
        }
        return card(body, CardStyle.SUBTLE);
    }

    private JPanel alternativesCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(insights.getAlternativeHeading()));
        body.add(gap(UiTokens.SM));
        if (insights.getAlternatives().isEmpty())
            body.add(labelHtml("No relevant improvement is currently ranked.", UiTokens.TEXT_MUTED));
        for (GoalDefinition alternative : insights.getAlternatives())
        {
            body.add(cardTitle(alternative.getTitle(), UiTokens.TEXT_PRIMARY));
            body.add(labelHtml(escape(alternative.getWhyItMatters()), UiTokens.TEXT_MUTED));
            body.add(gap(UiTokens.MD));
        }
        return card(body, CardStyle.SUBTLE);
    }

    private JPanel personalPathCard()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("PATH TO MY GOAL"));
        body.add(gap(UiTokens.SM));
        if (insights.getPersonalPath().isEmpty())
            body.add(labelHtml("Choose a primary goal to build this view.", UiTokens.TEXT_MUTED));
        for (GoalPathNode node : insights.getPersonalPath())
        {
            JPanel row = new JPanel(new BorderLayout(UiTokens.SM, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            boolean complete = node.getStatus().name().startsWith("COMPLETE");
            JLabel glyph = new JLabel(complete ? "✓" : "○");
            glyph.setForeground(complete ? UiTokens.SUCCESS : UiTokens.TEXT_SECONDARY);
            glyph.setFont(UiTokens.BODY);
            row.add(glyph, BorderLayout.WEST);
            JPanel copy = verticalPanel();
            copy.add(cardTitle(node.getGoal().getTitle(), complete ? UiTokens.TEXT_SECONDARY : UiTokens.TEXT_PRIMARY));
            copy.add(labelHtml(escape(node.getStatus().getLabel()), UiTokens.TEXT_MUTED));
            row.add(copy, BorderLayout.CENTER);
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.XS, 0));
            tags.setOpaque(false);
            if (node.isRng()) tags.add(badge("RNG", UiTokens.WARNING));
            if (node.isManual()) tags.add(badge("MANUAL", UiTokens.UNKNOWN));
            row.add(tags, BorderLayout.EAST);
            body.add(row);
            body.add(gap(UiTokens.SM));
        }
        return card(body, CardStyle.STANDARD);
    }

    private static Color healthColor(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.STRONG) return UiTokens.SUCCESS.brighter();
        if (level == AccountNeedLevel.GOOD) return UiTokens.SUCCESS;
        if (level == AccountNeedLevel.DEVELOPING) return UiTokens.WARNING;
        if (level == AccountNeedLevel.WEAK) return new Color(199, 119, 87);
        return UiTokens.UNKNOWN;
    }

    private static Color blockerColor(GoalBlocker blocker)
    {
        return blocker.getKind() == GoalBlocker.Kind.HARD_REQUIREMENT ? UiTokens.DANGER
            : blocker.getKind() == GoalBlocker.Kind.RECOMMENDED_PREPARATION ? UiTokens.WARNING
            : UiTokens.UNKNOWN;
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
