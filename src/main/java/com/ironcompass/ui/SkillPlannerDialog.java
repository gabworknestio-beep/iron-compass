package com.ironcompass.ui;

import com.ironcompass.integration.WikiBridge;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.PlannerPreferenceStore;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import com.ironcompass.training.BankedGoalBreakdown;
import com.ironcompass.training.BankedGoalProjection;
import com.ironcompass.training.BankedGoalService;
import com.ironcompass.training.BankedGoalStatus;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodDefinition;
import com.ironcompass.training.MethodPlannerService;
import com.ironcompass.training.MethodRecommendation;
import com.ironcompass.training.MethodResourceStatus;
import com.ironcompass.training.SkillTrainingPlan;
import com.ironcompass.training.TrainingMilestone;
import com.ironcompass.training.TrainingPlanSegment;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static com.ironcompass.ui.UiComponents.*;

/** Compact, read-only projection of the shared Skill Planner engine. */
final class SkillPlannerDialog
{
    private final Component owner;
    private final IronmanMethodCatalog catalog;
    private final MethodPlannerService planner;
    private final AccountState state;
    private final PlannerPreferenceStore preferences;
    private final List<GoalPlanProjection> activeGoals;
    private final WikiBridge wiki;
    private final BankedGoalService bankedGoals = new BankedGoalService();
    private final JComboBox<String> skill;
    private final JComboBox<Integer> target = new JComboBox<>();
    private final JTextField search = textField("Search methods, resources, or styles...");
    private final JPanel planHost = scrollableVerticalPanel();
    private final JPanel root = new JPanel(new BorderLayout(0, UiTokens.MD));
    private JDialog dialog;
    private boolean fullGuide;
    private boolean expanded;

    SkillPlannerDialog(Component owner, IronmanMethodCatalog catalog, MethodPlannerService planner,
                       AccountState state, PlannerPreferenceStore preferences,
                       List<GoalPlanProjection> activeGoals, WikiBridge wiki,
                       String initialSkill, int initialTarget, boolean fullGuide)
    {
        this.owner = owner;
        this.catalog = catalog;
        this.planner = planner;
        this.state = state;
        this.preferences = preferences;
        this.activeGoals = activeGoals == null ? Collections.emptyList() : activeGoals;
        this.wiki = wiki;
        this.fullGuide = fullGuide;
        skill = new JComboBox<>(catalog.getFullGuideSkills().toArray(new String[0]));
        for (int level = 2; level <= 99; level++) target.addItem(level);
        selectSkill(initialSkill);
        int current = Math.max(1, state.skillLevel((String) skill.getSelectedItem()));
        target.setSelectedItem(Math.max(current, Math.min(99, initialTarget < 2 ? 99 : initialTarget)));
        build();
    }

    void showDialog()
    {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        Window window = SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(window, "Iron Compass · Skill Planner", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(430, 590));
        dialog.setPreferredSize(new Dimension(470, 680));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    JPanel contentForTesting() { return root; }

    private void build()
    {
        root.setBackground(UiTokens.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(UiTokens.LG, UiTokens.LG, UiTokens.LG, UiTokens.LG));
        root.getAccessibleContext().setAccessibleName("Iron Compass Ironman Skill Planner");

        JPanel top = verticalPanel();
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (compassIcon() != null)
        {
            JLabel icon = new JLabel(compassIcon());
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UiTokens.SM));
            brand.add(icon);
        }
        JLabel title = new JLabel("IRONMAN SKILL PLANNER");
        title.setFont(UiTokens.APP_TITLE);
        title.setForeground(UiTokens.ACCENT);
        brand.add(title);
        top.add(brand);
        top.add(labelHtml("Account-aware training from your current level to a useful target.",
            UiTokens.TEXT_MUTED));
        top.add(gap(UiTokens.MD));
        JPanel selectors = new JPanel(new GridLayout(1, 2, UiTokens.SM, 0));
        selectors.setOpaque(false);
        selectors.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleComboBox(skill);
        styleComboBox(target);
        skill.getAccessibleContext().setAccessibleName("Skill");
        target.getAccessibleContext().setAccessibleName("Target level");
        selectors.add(skill);
        selectors.add(target);
        selectors.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CONTROL_HEIGHT));
        top.add(selectors);
        top.add(gap(UiTokens.SM));
        search.getAccessibleContext().setAccessibleName("Search training methods");
        top.add(search);
        root.add(top, BorderLayout.NORTH);

        JScrollPane scroll = scrollPane(planHost);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.SM, 0));
        footer.setOpaque(false);
        JButton full = smallButton("FULL 1–99 GUIDE");
        full.addActionListener(event -> { fullGuide = true; refresh(); });
        JButton close = primaryButton("DONE");
        close.addActionListener(event ->
        {
            Window window = SwingUtilities.getWindowAncestor(root);
            if (window != null) window.dispose();
        });
        footer.add(full);
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);

        skill.addActionListener(event ->
        {
            fullGuide = false;
            int current = Math.max(1, state.skillLevel((String) skill.getSelectedItem()));
            if ((Integer) target.getSelectedItem() < current) target.setSelectedItem(current);
            refresh();
        });
        target.addActionListener(event -> { fullGuide = false; refresh(); });
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { refresh(); }
            @Override public void removeUpdate(DocumentEvent event) { refresh(); }
            @Override public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        refresh();
    }

    private void refresh()
    {
        String selectedSkill = (String) skill.getSelectedItem();
        if (selectedSkill == null || target.getSelectedItem() == null) return;
        int current = Math.max(1, state.skillLevel(selectedSkill));
        int requested = (Integer) target.getSelectedItem();
        SkillTrainingPlan plan = fullGuide
            ? planner.fullGuide(catalog, selectedSkill, state, preferences, activeGoals)
            : planner.plan(catalog, selectedSkill, current, Math.max(current, requested), state, preferences,
                activeGoals);
        rebuildPlan(plan);
    }

    private void rebuildPlan(SkillTrainingPlan plan)
    {
        planHost.removeAll();
        planHost.setBorder(BorderFactory.createEmptyBorder(UiTokens.XS, UiTokens.XS, UiTokens.XS, UiTokens.XS));
        if (plan == null)
        {
            planHost.add(card(labelHtml("No researched guide is bundled for this skill yet.", UiTokens.UNKNOWN)));
            finishRefresh();
            return;
        }

        JPanel summary = verticalPanel();
        summary.add(sectionLabel(fullGuide ? "FULL GUIDE" : "YOUR PLAN"));
        summary.add(gap(UiTokens.XS));
        summary.add(cardTitle(plan.getSkill().toUpperCase(Locale.ENGLISH) + "  " + plan.getCurrentLevel()
            + " → " + plan.getTargetLevel(), UiTokens.ACCENT_HOVER));
        summary.add(labelHtml(NumberFormat.getIntegerInstance(Locale.ENGLISH).format(plan.getXpRemaining())
            + " XP remaining · " + escape(plan.getEstimatedTime()), UiTokens.TEXT_MUTED));
        planHost.add(card(summary, CardStyle.HERO));

        addBankedGoal(plan);

        if (plan.isComplete())
        {
            planHost.add(gap(UiTokens.MD));
            planHost.add(card(labelHtml("This target is already complete on the current snapshot.",
                UiTokens.SUCCESS), CardStyle.SUCCESS));
        }
        int index = 0;
        for (TrainingPlanSegment segment : plan.getSegments())
        {
            planHost.add(gap(UiTokens.MD));
            planHost.add(segmentCard(segment, index++ == 0));
        }
        addMilestones(plan);
        addSearchResults(plan.getSkill());
        planHost.add(gap(UiTokens.MD));
        JButton disclosure = ghostButton(expanded ? "LESS DETAIL" : "MORE DETAILS");
        disclosure.addActionListener(event -> { expanded = !expanded; rebuildPlan(plan); });
        planHost.add(disclosure);
        finishRefresh();
    }

    private void addBankedGoal(SkillTrainingPlan plan)
    {
        int targetLevel = fullGuide ? 99 : plan.getTargetLevel();
        BankedGoalProjection projection = bankedGoals.project(state, plan.getSkill(), targetLevel);
        JPanel body = verticalPanel();
        JPanel heading = new JPanel(new BorderLayout(UiTokens.SM, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(sectionLabel("BANK-TO-GOAL"), BorderLayout.WEST);
        heading.add(badge(bankedBadge(projection), bankedColor(projection)), BorderLayout.EAST);
        body.add(heading);
        body.add(gap(UiTokens.SM));

        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ENGLISH);
        switch (projection.getStatus())
        {
            case UNKNOWN:
                body.add(cardTitle("OPEN YOUR BANK TO CALCULATE", UiTokens.UNKNOWN));
                break;
            case NOT_SUPPORTED:
                body.add(cardTitle("NO HONEST BANKED-XP TOTAL", UiTokens.TEXT_SECONDARY));
                break;
            case COMPLETE:
                body.add(cardTitle("TARGET ALREADY COMPLETE", UiTokens.SUCCESS));
                break;
            case NO_RESOURCES:
                body.add(cardTitle("NO RECOGNIZED RESOURCES", UiTokens.TEXT_PRIMARY));
                break;
            case READY:
                body.add(cardTitle("TARGET BANKED · ~" + numbers.format(projection.getRecognizedXp())
                    + " XP", UiTokens.SUCCESS));
                addBankedProgress(body, projection);
                break;
            case IN_PROGRESS:
                body.add(cardTitle(projection.getProgressPercent() + "% BANKED · ~"
                    + numbers.format(projection.getRecognizedXp()) + " XP", UiTokens.ACCENT_HOVER));
                addBankedProgress(body, projection);
                break;
            default:
                break;
        }
        body.add(gap(UiTokens.SM));
        body.add(labelHtml(escape(projection.getExplanation()), UiTokens.TEXT_MUTED));

        if (expanded && !projection.getBreakdown().isEmpty())
        {
            body.add(gap(UiTokens.MD));
            body.add(sectionLabel("RECOGNIZED RESOURCES"));
            int limit = Math.min(8, projection.getBreakdown().size());
            for (int index = 0; index < limit; index++)
            {
                BankedGoalBreakdown line = projection.getBreakdown().get(index);
                body.add(labelHtml("•  " + numbers.format(line.getActions()) + " × "
                    + escape(line.getLabel()) + " · ~" + numbers.format(line.getExperience()) + " XP",
                    UiTokens.TEXT_SECONDARY));
            }
            if (projection.getBreakdown().size() > limit)
                body.add(labelHtml("+ " + (projection.getBreakdown().size() - limit)
                    + " other recognized conversions", UiTokens.MUTED));
        }
        planHost.add(gap(UiTokens.MD));
        planHost.add(card(body, projection.getStatus() == BankedGoalStatus.READY
            ? CardStyle.SUCCESS : CardStyle.SUBTLE));
    }

    private static void addBankedProgress(JPanel body, BankedGoalProjection projection)
    {
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(projection.getProgressPercent());
        progress.setPreferredSize(new Dimension(200, 6));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progress.setForeground(projection.getStatus() == BankedGoalStatus.READY
            ? UiTokens.SUCCESS : UiTokens.ACCENT);
        progress.setBackground(UiTokens.SURFACE);
        progress.setBorderPainted(false);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(gap(UiTokens.SM));
        body.add(progress);
        body.add(gap(UiTokens.XS));
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ENGLISH);
        int shortfall = Math.max(0, projection.getXpRemaining() - projection.getRecognizedXp());
        String summary = projection.getStatus() == BankedGoalStatus.READY
            ? "Recognized resources cover this target · estimated level " + projection.getProjectedLevel()
            : "Estimated level " + projection.getProjectedLevel() + " · ~" + numbers.format(shortfall)
                + " XP still needed";
        body.add(labelHtml(summary, UiTokens.TEXT_SECONDARY));
    }

    private static String bankedBadge(BankedGoalProjection projection)
    {
        switch (projection.getStatus())
        {
            case UNKNOWN: return "UNKNOWN";
            case NOT_SUPPORTED: return "NOT BANKABLE";
            case COMPLETE: return "COMPLETE";
            case READY: return "READY · ESTIMATE";
            default: return "ESTIMATE";
        }
    }

    private static java.awt.Color bankedColor(BankedGoalProjection projection)
    {
        switch (projection.getStatus())
        {
            case UNKNOWN: return UiTokens.UNKNOWN;
            case COMPLETE:
            case READY: return UiTokens.SUCCESS;
            case NOT_SUPPORTED: return UiTokens.MUTED;
            default: return UiTokens.ACCENT;
        }
    }

    private JPanel segmentCard(TrainingPlanSegment segment, boolean first)
    {
        MethodRecommendation recommendation = segment.getRecommendation();
        IronmanMethodDefinition method = recommendation.getRecommended();
        JPanel body = verticalPanel();
        JPanel heading = new JPanel(new BorderLayout(UiTokens.SM, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(sectionLabel(first ? "RECOMMENDED" : "THEN"), BorderLayout.WEST);
        heading.add(badge(segment.getFromLevel() + " → " + segment.getToLevel(), UiTokens.ACCENT),
            BorderLayout.EAST);
        body.add(heading);
        body.add(gap(UiTokens.SM));
        body.add(cardTitle(method.getTitle(), first ? UiTokens.ACCENT_HOVER : UiTokens.TEXT_PRIMARY));
        body.add(labelHtml(escape(recommendation.getXpRateSummary()) + " · "
            + humanize(method.getAttention()) + " intensity", UiTokens.TEXT_MUTED));
        body.add(gap(UiTokens.SM));
        body.add(labelHtml(escape(recommendation.getReason()), UiTokens.TEXT_SECONDARY));

        if (recommendation.getRequirementStatus() == TruthValue.UNKNOWN)
            body.add(statusLine(TruthValue.UNKNOWN, "One or more access requirements are not confirmed."));
        MethodResourceStatus resource = recommendation.getResourceStatus();
        if (resource == MethodResourceStatus.UNKNOWN || resource == MethodResourceStatus.EMPTY
            || resource == MethodResourceStatus.PARTIAL)
        {
            TruthValue status = resource == MethodResourceStatus.EMPTY ? TruthValue.FALSE : TruthValue.UNKNOWN;
            body.add(gap(UiTokens.SM));
            body.add(statusLine(status, recommendation.getResourceSummary()));
        }
        if (expanded)
        {
            addDetailLines(body, "WHY USE IT", method.getBenefits().isEmpty()
                ? Collections.singletonList(method.getDescription()) : method.getBenefits());
            addDetailLines(body, "RECOMMENDED SETUP", method.getRecommendedRequirements());
            addDetailLines(body, "RESOURCES / COST", combine(method.getConsumes(), method.getCosts()));
            addDetailLines(body, "SUPPLY CHAIN", method.getAcquisitionSources());
            addDetailLines(body, "OUTPUTS", method.getUsefulOutputs());
            if (!recommendation.getAlternatives().isEmpty())
                addDetailLines(body, "ALTERNATIVES", titles(recommendation.getAlternatives(), false));
            if (!recommendation.getLockedAlternatives().isEmpty())
                addDetailLines(body, "LOCKED OPTIONS", titles(recommendation.getLockedAlternatives(), true));
        }
        JButton source = smallButton("VIEW METHOD");
        source.setAlignmentX(Component.LEFT_ALIGNMENT);
        source.addActionListener(event -> wiki.open(method.getWikiPage()));
        body.add(gap(UiTokens.SM));
        body.add(source);
        return card(body, first ? CardStyle.HERO : CardStyle.STANDARD);
    }

    private void addMilestones(SkillTrainingPlan plan)
    {
        if (plan.getMilestones().isEmpty()) return;
        planHost.add(gap(UiTokens.MD));
        JPanel body = verticalPanel();
        body.add(sectionLabel("TARGET UNLOCKS"));
        body.add(gap(UiTokens.SM));
        int limit = expanded ? plan.getMilestones().size() : Math.min(5, plan.getMilestones().size());
        for (int index = 0; index < limit; index++)
        {
            TrainingMilestone milestone = plan.getMilestones().get(index);
            body.add(cardTitle(milestone.getLevel() + " · " + milestone.getTitle(), UiTokens.TEXT_PRIMARY));
            body.add(labelHtml(escape(milestone.getIronmanValue()), UiTokens.TEXT_MUTED));
            if (index + 1 < limit) body.add(gap(UiTokens.SM));
        }
        if (plan.getMilestones().size() > limit)
            body.add(labelHtml("+ " + (plan.getMilestones().size() - limit) + " later milestones", UiTokens.MUTED));
        planHost.add(card(body, CardStyle.SUBTLE));
    }

    private void addSearchResults(String selectedSkill)
    {
        String query = search.getText().trim();
        if (query.isEmpty()) return;
        JPanel body = verticalPanel();
        body.add(sectionLabel("METHOD SEARCH"));
        int shown = 0;
        for (IronmanMethodDefinition method : catalog.search(query))
        {
            if (!method.getSkill().equalsIgnoreCase(selectedSkill)) continue;
            if (shown++ > 0) body.add(gap(UiTokens.SM));
            body.add(cardTitle(method.getTitle(), UiTokens.TEXT_PRIMARY));
            body.add(labelHtml((method.isTrainingMethod() ? method.getMinLevel() + "–" + method.getMaxLevel()
                : "SUPPLY CHAIN") + " · " + escape(method.getDescription()), UiTokens.TEXT_MUTED));
            if (shown == 6) break;
        }
        if (shown == 0) body.add(labelHtml("No matching method in this pilot guide.", UiTokens.TEXT_MUTED));
        planHost.add(gap(UiTokens.MD));
        planHost.add(card(body, CardStyle.SUBTLE));
    }

    private static void addDetailLines(JPanel body, String heading, List<String> lines)
    {
        if (lines.isEmpty()) return;
        body.add(gap(UiTokens.MD));
        body.add(sectionLabel(heading));
        for (String line : lines) body.add(labelHtml("•  " + escape(line), UiTokens.TEXT_SECONDARY));
    }

    private static List<String> combine(List<String> first, List<String> second)
    {
        java.util.ArrayList<String> result = new java.util.ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private static List<String> titles(List<IronmanMethodDefinition> methods, boolean locked)
    {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (IronmanMethodDefinition method : methods)
        {
            String requirement = method.getRequirements() == null ? ""
                : " — requires " + method.getRequirements().getLabel();
            result.add((locked ? "LOCKED · " : "") + method.getTitle() + requirement);
        }
        return result;
    }

    private void selectSkill(String initial)
    {
        if (initial == null) return;
        for (int index = 0; index < skill.getItemCount(); index++)
            if (skill.getItemAt(index).equalsIgnoreCase(initial))
            {
                skill.setSelectedIndex(index);
                return;
            }
    }

    private void finishRefresh()
    {
        planHost.revalidate();
        planHost.repaint();
    }

    private static String humanize(String value)
    {
        if (value == null || value.isEmpty()) return "Variable";
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
