package com.ironpath.ui;

import com.ironpath.IronPathConfig;
import com.ironpath.gear.GearEvaluation;
import com.ironpath.gear.GearPreferenceStore;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearStatus;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.goal.GoalAction;
import com.ironpath.goal.GoalResolution;
import com.ironpath.integration.QuestHelperBridge;
import com.ironpath.integration.ShortestPathBridge;
import com.ironpath.integration.WikiBridge;
import com.ironpath.persistence.ManualOverride;
import com.ironpath.persistence.ManualOverrideStore;
import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import com.ironpath.route.PreparationEvaluation;
import com.ironpath.route.PreparationStatus;
import com.ironpath.route.RouteProjection;
import com.ironpath.route.RouteChapterProgress;
import com.ironpath.route.RouteJourney;
import com.ironpath.route.RouteJourneyService;
import com.ironpath.route.StepEvaluation;
import com.ironpath.route.StepStatus;
import com.ironpath.route.StepType;
import com.ironpath.route.WhileHereSpec;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.supply.SupplyForecast;
import com.ironpath.supply.SupplyLine;
import com.ironpath.training.SkillTrainingAdvisor;
import com.ironpath.training.TrainingAdvice;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public final class IronPathPanel extends PluginPanel
{
    private static final SkillTrainingAdvisor TRAINING_ADVISOR = new SkillTrainingAdvisor();
    private static final RouteJourneyService JOURNEY_SERVICE = new RouteJourneyService();
    private static final String HOME = "home";
    private static final String BROWSER = "browser";
    private static final String GEAR = "gear";

    private final IronPathConfig config;
    private final WikiBridge wikiBridge;
    private final ShortestPathBridge shortestPathBridge;
    private final QuestHelperBridge questHelperBridge;
    private final ManualOverrideStore persistence;
    private final GearPreferenceStore gearPreferences;
    private final Runnable reevaluate;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JPanel home = scrollableVerticalPanel();
    private final JPanel browserResults = scrollableVerticalPanel();
    private final GearPathPanel gearPanel;
    private final JTextField search = new JTextField();
    private final JButton pathBack = smallButton("OVERVIEW");
    private RouteProjection projection;
    private GearProjection gearProjection;
    private GoalResolution goalResolution;
    private SupplyForecast supplyForecast;
    private RouteJourney journey;
    private AccountState accountState = AccountState.loggedOut();
    private String routeDetailId;

    public IronPathPanel(IronPathConfig config, WikiBridge wikiBridge, ShortestPathBridge shortestPathBridge,
                         QuestHelperBridge questHelperBridge, ManualOverrideStore persistence, Runnable reevaluate)
    {
        this.config = config;
        this.wikiBridge = wikiBridge;
        this.shortestPathBridge = shortestPathBridge;
        this.questHelperBridge = questHelperBridge;
        this.persistence = persistence;
        this.gearPreferences = persistence instanceof GearPreferenceStore
            ? (GearPreferenceStore) persistence : new InMemoryGearPreferenceStore();
        this.reevaluate = reevaluate;
        this.gearPanel = new GearPathPanel(wikiBridge, gearPreferences, persistence, reevaluate,
            () -> cards.show(cardHost, HOME));

        setLayout(new BorderLayout());
        setBackground(UiTokens.BACKGROUND);
        cardHost.setBackground(UiTokens.BACKGROUND);
        cardHost.add(buildHomeScroll(), HOME);
        cardHost.add(buildBrowser(), BROWSER);
        cardHost.add(gearPanel, GEAR);
        add(cardHost, BorderLayout.CENTER);
        showIdle();
    }

    public void update(AccountState state, RouteProjection newProjection)
    {
        update(state, newProjection, null, null, null);
    }

    /** Compatibility entry point for older render tests and callers. */
    public void update(AccountState state, RouteProjection newProjection, RouteProjection newGearProjection)
    {
        update(state, newProjection, null, null, null);
    }

    public void update(AccountState state, RouteProjection newProjection, GearProjection newGearProjection,
                       GoalResolution newGoalResolution, SupplyForecast newSupplyForecast)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> update(state, newProjection, newGearProjection,
                newGoalResolution, newSupplyForecast));
            return;
        }
        accountState = state;
        projection = newProjection;
        journey = newProjection == null ? null : JOURNEY_SERVICE.project(newProjection);
        gearProjection = newGearProjection;
        goalResolution = newGoalResolution;
        supplyForecast = newSupplyForecast;
        rebuildHome();
        rebuildBrowser();
        gearPanel.update(accountState, gearProjection);
    }

    public void showError(String message)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> showError(message));
            return;
        }
        home.removeAll();
        home.add(sectionLabel("IRONPATH"));
        home.add(gap(8));
        home.add(card(labelHtml("<b>Route data could not be loaded.</b><br><br>" + escape(message), UiTokens.DANGER)));
        home.revalidate();
        home.repaint();
        cards.show(cardHost, HOME);
    }

    void showPathForTesting()
    {
        cards.show(cardHost, BROWSER);
    }

    void showGearForTesting(String style)
    {
        gearPanel.selectStyleForTesting(style);
        cards.show(cardHost, GEAR);
    }

    void showGearObjectiveForTesting(String objectiveId)
    {
        gearPanel.showObjective(objectiveId);
        cards.show(cardHost, GEAR);
    }

    private void rebuildHome()
    {
        home.removeAll();
        home.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (!accountState.isLoggedIn() || projection == null)
        {
            showIdle();
            return;
        }

        home.add(buildHeader());
        if (accountState.getAccountMode() == AccountMode.REGULAR)
        {
            home.add(gap(8));
            home.add(notice("IronPath is tuned for Ironman progression. Route preview is available, but this account is not an Ironman."));
        }
        else if (accountState.getAccountMode().isUltimate())
        {
            home.add(gap(8));
            home.add(notice("Ultimate Ironman detected. This route is not inventory-optimized for UIM; use it as a quest-order preview."));
        }

        home.add(gap(10));
        home.add(buildPosition());
        home.add(gap(10));

        GoalAction goalAction = goalResolution == null ? null : goalResolution.getNextAction();
        if (goalAction != null && goalAction.getRouteStep() != null)
        {
            home.add(buildGoalRouteStep(goalAction));
        }
        else if (goalAction != null && goalAction.getKind() != GoalAction.Kind.COMPLETE)
        {
            home.add(buildGoalAction(goalAction));
        }
        else if (projection.getCurrent() == null)
        {
            home.add(card(labelHtml("<b>Route complete.</b><br><br>Every required bundled step is satisfied or skipped.", UiTokens.SUCCESS)));
        }
        else
        {
            home.add(buildCurrent(projection.getCurrent()));
        }

        if (gearProjection != null && gearProjection.getRecommended() != null)
        {
            home.add(gap(10));
            home.add(buildNextGear(gearProjection.getRecommended()));
        }

        if (projection.getCurrent() != null)
        {
            home.add(gap(10));
            if (!projection.getUpcoming().isEmpty())
            {
                home.add(buildUpcoming(projection.getUpcoming()));
                home.add(gap(10));
            }
            if (supplyForecast != null)
            {
                home.add(buildSupplyForecast(supplyForecast));
                home.add(gap(10));
            }
            if (!projection.getPreparation().isEmpty())
            {
                home.add(buildPreparation(projection.getPreparation()));
                home.add(gap(10));
            }
            if (!projection.getCurrent().getStep().getWhileHere().isEmpty())
            {
                home.add(buildWhileHere(projection.getCurrent().getStep().getWhileHere()));
                home.add(gap(10));
            }
        }
        home.add(buildFooter());
        home.add(verticalGlue());
        home.revalidate();
        home.repaint();
    }

    private JPanel buildHeader()
    {
        JPanel panel = verticalPanel();
        panel.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("IRONPATH");
        title.setForeground(UiTokens.ACCENT);
        title.setFont(UiTokens.TITLE);
        JLabel percentage = new JLabel(String.format(Locale.ENGLISH, "%.0f%%", projection.getProgressPercent()));
        percentage.setForeground(UiTokens.TEXT);
        percentage.setFont(UiTokens.BODY);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(percentage, BorderLayout.EAST);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleRow.getPreferredSize().height));
        panel.add(titleRow);

        RouteChapterProgress chapter = journey == null ? null : journey.getCurrent();
        String context = chapter == null ? projection.getRoute().getName()
            : chapter.getChapter().getName() + " " + chapter.getCompleteCount() + "/" + chapter.getTotalCount();
        JLabel route = labelHtml(escape(context) + "<br>Overall " + projection.getCompleteCount()
            + "/" + projection.getTotalCount() + "  ·  " + escape(humanize(accountState.getAccountMode().name())),
            UiTokens.MUTED);
        panel.add(route);
        panel.add(gap(5));
        JProgressBar progress = new JProgressBar(0, Math.max(1, projection.getTotalCount()));
        progress.setValue(projection.getCompleteCount());
        progress.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 6));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progress.setForeground(UiTokens.ACCENT);
        progress.setBackground(UiTokens.CARD);
        progress.setBorderPainted(false);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(progress);
        return panel;
    }

    private JPanel buildPosition()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("YOU ARE HERE"));
        body.add(gap(5));
        RouteChapterProgress current = journey == null ? null : journey.getCurrent();
        if (current == null)
        {
            body.add(labelHtml("<b>Route position unavailable</b>", UiTokens.UNKNOWN));
            return card(body);
        }
        body.add(labelHtml("<b>" + escape(current.getChapter().getName()) + "</b>  "
            + current.getCompleteCount() + "/" + current.getTotalCount(), UiTokens.ACCENT));
        body.add(gap(4));
        body.add(labelHtml(escape(current.getChapter().getDescription()), UiTokens.MUTED));
        body.add(gap(6));
        int currentIndex = journey.getChapters().indexOf(current);
        if (currentIndex > 0)
        {
            RouteChapterProgress previous = journey.getChapters().get(currentIndex - 1);
            body.add(labelHtml("✓  " + escape(previous.getChapter().getName()), UiTokens.SUCCESS));
        }
        body.add(labelHtml("→  " + escape(current.getChapter().getName()), UiTokens.ACCENT));
        if (currentIndex + 1 < journey.getChapters().size())
        {
            body.add(labelHtml("○  " + escape(journey.getChapters().get(currentIndex + 1).getChapter().getName()),
                UiTokens.TEXT));
        }
        return card(body);
    }

    private JPanel buildCurrentGoal(GoalResolution resolution)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("CURRENT GOAL"));
        body.add(gap(5));
        body.add(labelHtml("<b>" + escape(resolution.getGoal().getUpgrade().getName()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(humanize(resolution.getGoal().getStatus().name()) + "  ·  "
            + resolution.getGoal().getUpgrade().getRole().name().toLowerCase(Locale.ENGLISH), UiTokens.MUTED));
        body.add(gap(6));
        body.add(labelHtml(escape(resolution.getGoal().getUpgrade().getWhy()), UiTokens.TEXT));
        body.add(gap(7));
        JButton gear = smallButton("OPEN GEAR GOAL");
        gear.setAlignmentX(Component.LEFT_ALIGNMENT);
        gear.addActionListener(event ->
        {
            gearPanel.showObjective(resolution.getGoal().getUpgrade().getId());
            cards.show(cardHost, GEAR);
        });
        body.add(gear);
        return card(body);
    }

    private JPanel buildGoalRouteStep(GoalAction action)
    {
        StepEvaluation evaluation = action.getRouteStep();
        JPanel body = verticalPanel();
        body.add(sectionLabel("DO THIS NOW"));
        body.add(gap(6));
        JLabel title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b>", UiTokens.ACCENT);
        title.setFont(UiTokens.STEP_TITLE);
        body.add(title);
        body.add(labelHtml("Goal dependency  ·  " + escape(evaluation.getStep().getCategory()), UiTokens.MUTED));
        body.add(gap(7));
        body.add(labelHtml(escape(evaluation.getStep().getInstruction()), UiTokens.TEXT));
        addQuestHelperHint(body, evaluation);
        addTrainingAdvice(body, evaluation);
        body.add(gap(9));
        body.add(sectionLabel("WHY THIS NOW"));
        body.add(gap(3));
        body.add(labelHtml(escape(evaluation.getStep().getReason()), UiTokens.TEXT));
        body.add(gap(3));
        body.add(labelHtml(escape(action.getExplanation()), UiTokens.MUTED));
        addUnlocks(body, evaluation);
        body.add(gap(9));
        body.add(sectionLabel(readinessTitle(evaluation)));
        body.add(gap(3));
        if (evaluation.getReadinessDetails().isEmpty())
        {
            body.add(statusLine(evaluation.getReadiness(), "Route position and known requirements"));
        }
        else
        {
            for (RequirementResult detail : evaluation.getReadinessDetails())
            {
                body.add(statusLine(detail.getValue(), detail.getLabel() + detailSuffix(detail)));
            }
        }
        body.add(gap(9));
        body.add(actionRow(evaluation));
        return card(body);
    }

    private JPanel buildGoalAction(GoalAction action)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("DO THIS NOW"));
        body.add(gap(6));
        body.add(labelHtml("<b>" + escape(action.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml("Selected gear dependency", UiTokens.MUTED));
        body.add(gap(7));
        body.add(labelHtml(escape(action.getExplanation()), UiTokens.TEXT));
        body.add(gap(9));
        body.add(sectionLabel("WHAT THIS UNLOCKS"));
        body.add(gap(3));
        body.add(labelHtml(action.getGearStep() == null ? "Advances the selected dependency chain."
            : "Advances the selected " + escape(action.getGearStep().getUpgrade().getName())
                + " dependency chain.", UiTokens.MUTED));
        if (action.getGearStep() != null && !action.getGearStep().getMissingReasons().isEmpty())
        {
            body.add(gap(8));
            int shown = 0;
            for (String missing : action.getGearStep().getMissingReasons())
            {
                if (shown++ == 3) break;
                body.add(statusLine(TruthValue.FALSE, missing));
            }
        }
        body.add(gap(8));
        JButton gear = smallButton("VIEW REQUIREMENTS");
        gear.setAlignmentX(Component.LEFT_ALIGNMENT);
        gear.addActionListener(event ->
        {
            if (action.getGearStep() != null)
            {
                gearPanel.showObjective(action.getGearStep().getUpgrade().getId());
            }
            cards.show(cardHost, GEAR);
        });
        body.add(gear);
        return card(body);
    }

    private JPanel buildNextGear(GearEvaluation evaluation)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(evaluation.getUpgrade().getRole().name().equals("RECOMMENDED")
            ? "GEAR OPPORTUNITY" : "OPTIONAL GEAR DETOUR"));
        body.add(gap(5));
        body.add(labelHtml("<b>" + escape(evaluation.getUpgrade().getName()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(priorityLabel(evaluation) + "  ·  "
            + humanize(evaluation.getUpgrade().getSlot().name()), UiTokens.MUTED));
        body.add(gap(6));
        body.add(labelHtml(escape(evaluation.getUpgrade().getWhy()), UiTokens.TEXT));
        body.add(gap(7));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton doNow = smallButton(evaluation.isSelectedGoal() ? "GOAL SET" : "DO THIS NOW");
        doNow.addActionListener(event ->
        {
            gearPreferences.setSelectedGoalId(evaluation.getUpgrade().getId());
            reevaluate.run();
        });
        JButton gear = smallButton("VIEW");
        gear.addActionListener(event ->
        {
            gearPanel.showObjective(evaluation.getUpgrade().getId());
            cards.show(cardHost, GEAR);
        });
        actions.add(doNow);
        actions.add(gear);
        body.add(actions);
        return card(body);
    }

    private JPanel buildSupplyForecast(SupplyForecast forecast)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("SUPPLY STATUS"));
        body.add(gap(4));
        body.add(labelHtml("For " + escape(forecast.getGoal().getUpgrade().getName()), UiTokens.MUTED));
        body.add(gap(5));
        if (forecast.getLines().isEmpty())
        {
            body.add(statusLine(TruthValue.TRUE, "No special preparation required"));
        }
        else
        {
            for (SupplyLine line : forecast.getLines())
            {
                String estimate = line.isEstimated() ? " estimated" : "";
                body.add(statusLine(line.getStatus(), line.getName() + ": " + line.getActualUnits()
                    + " / " + line.getRequiredUnits() + " " + line.getUnitLabel() + estimate));
            }
        }
        return card(body);
    }

    private JPanel buildLongTermPath()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("LONG-TERM PATH"));
        body.add(gap(5));
        if (goalResolution != null && goalResolution.getDependencyPath().size() > 1)
        {
            int index = 0;
            for (String dependency : goalResolution.getDependencyPath())
            {
                body.add(labelHtml((index++ == 0 ? "→  " : "○  ") + escape(dependency), UiTokens.TEXT));
            }
        }
        else
        {
            int shown = 0;
            int recommendedTier = gearProjection.getRecommended() == null ? 0
                : gearProjection.getRecommended().getUpgrade().getTier();
            for (GearEvaluation evaluation : gearProjection.getEvaluations())
            {
                if (evaluation.getUpgrade().getTier() <= recommendedTier
                    || evaluation.getStatus() == GearStatus.OWNED
                    || evaluation.getStatus() == GearStatus.SKIPPED)
                {
                    continue;
                }
                body.add(labelHtml("○  " + escape(evaluation.getUpgrade().getName()), UiTokens.TEXT));
                if (++shown == 3) break;
            }
            if (shown == 0) body.add(labelHtml("Choose any Gear objective to build a dependency path.", UiTokens.MUTED));
        }
        return card(body);
    }

    private JPanel buildCurrent(StepEvaluation evaluation)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(evaluation.getStatus() == StepStatus.CURRENT ? "DO THIS NOW"
            : "STEP DETAIL  ·  " + humanize(evaluation.getStatus().name()).toUpperCase(Locale.ENGLISH)));
        body.add(gap(6));
        JLabel title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b>", UiTokens.ACCENT);
        title.setFont(UiTokens.STEP_TITLE);
        body.add(title);
        JLabel category = new JLabel(evaluation.getStep().getCategory() + riskSuffix(evaluation));
        category.setForeground(UiTokens.MUTED);
        category.setFont(UiTokens.LABEL);
        category.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(category);
        body.add(gap(7));
        body.add(labelHtml(escape(evaluation.getStep().getInstruction()), UiTokens.TEXT));
        addQuestHelperHint(body, evaluation);
        addTrainingAdvice(body, evaluation);
        body.add(gap(9));
        body.add(sectionLabel("WHY THIS NOW"));
        body.add(gap(3));
        body.add(labelHtml(escape(evaluation.getStep().getReason()), UiTokens.MUTED));
        addUnlocks(body, evaluation);
        body.add(gap(9));
        body.add(sectionLabel(readinessTitle(evaluation)));
        body.add(gap(3));
        if (evaluation.getReadinessDetails().isEmpty())
        {
            body.add(statusLine(evaluation.getReadiness(), "Route position and known requirements"));
        }
        else
        {
            for (RequirementResult detail : evaluation.getReadinessDetails())
            {
                body.add(statusLine(detail.getValue(), detail.getLabel() + detailSuffix(detail)));
            }
        }
        if (evaluation.getCompletion() == TruthValue.UNKNOWN
            && evaluation.getStep().getCompletion() != null
            && "MANUAL_ONLY".equalsIgnoreCase(evaluation.getStep().getCompletion().getType()))
        {
            String label = evaluation.getStep().getCompletion().getLabel();
            body.add(statusLine(TruthValue.UNKNOWN, "Confirm when: "
                + (label == null || label.isEmpty() ? "this authored milestone is complete" : label)));
        }
        body.add(gap(9));
        body.add(actionRow(evaluation));
        return card(body);
    }

    private void addUnlocks(JPanel body, StepEvaluation evaluation)
    {
        body.add(gap(9));
        body.add(sectionLabel("WHAT THIS UNLOCKS"));
        body.add(gap(3));
        GearEvaluation linked = null;
        if (gearProjection != null)
        {
            for (GearEvaluation candidate : gearProjection.getEvaluations())
            {
                if (candidate.getUpgrade().getRouteStepIds().contains(evaluation.getStep().getId())
                    && candidate.getStatus() != GearStatus.OWNED)
                {
                    linked = candidate;
                    break;
                }
            }
        }
        body.add(labelHtml(linked == null
            ? "Progresses this chapter and removes a dependency from the route ahead."
            : "Gear path: " + escape(linked.getUpgrade().getName()) + ".", UiTokens.MUTED));
        body.add(gap(7));
        body.add(sectionLabel("WHAT COMES NEXT"));
        body.add(gap(3));
        StepEvaluation next = nextAfter(evaluation);
        body.add(labelHtml(next == null ? "Finish the remaining route objectives."
            : "○  " + escape(next.getStep().getTitle()), UiTokens.TEXT));
    }

    private StepEvaluation nextAfter(StepEvaluation current)
    {
        boolean found = false;
        for (StepEvaluation candidate : projection.getSteps())
        {
            if (found && candidate.getStatus() != StepStatus.COMPLETE
                && candidate.getStatus() != StepStatus.SKIPPED_MANUALLY
                && candidate.getStatus() != StepStatus.OPTIONAL)
            {
                return candidate;
            }
            if (candidate.getStep().getId().equals(current.getStep().getId())) found = true;
        }
        return null;
    }

    private void addTrainingAdvice(JPanel body, StepEvaluation evaluation)
    {
        TrainingAdvice advice = TRAINING_ADVISOR.advise(evaluation.getStep(), accountState);
        if (advice == null) return;
        body.add(gap(8));
        body.add(sectionLabel("TRAINING METHOD"));
        body.add(gap(3));
        body.add(labelHtml(escape(advice.getPrimaryMethod()), UiTokens.TEXT));
        body.add(gap(3));
        body.add(labelHtml("Alternative: " + escape(advice.getAlternativeMethod()), UiTokens.MUTED));
        body.add(gap(3));
        body.add(labelHtml(escape(advice.getBankContext()), UiTokens.UNKNOWN));
    }

    private void addQuestHelperHint(JPanel body, StepEvaluation evaluation)
    {
        String helper = evaluation.getStep().getQuestHelperKey();
        if (helper == null || helper.isEmpty() || questHelperBridge.canLaunch()) return;
        body.add(gap(4));
        body.add(labelHtml("Quest Helper: search <b>" + escape(helper)
            + "</b> in its sidebar.", UiTokens.MUTED));
    }

    private JPanel actionRow(StepEvaluation evaluation)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (config.wikiActions() && evaluation.getStep().getWikiPage() != null)
        {
            JButton wiki = smallButton("WIKI");
            wiki.setToolTipText("Open the contextual OSRS Wiki page");
            wiki.addActionListener(event -> wikiBridge.open(evaluation.getStep().getWikiPage()));
            row.add(wiki);
        }
        if (config.shortestPath() && shortestPathBridge != null
            && evaluation.getStep().getType() != StepType.QUEST
            && evaluation.getStep().getLocation() != null && shortestPathBridge.isAvailable())
        {
            JButton path = smallButton("PATH");
            path.setToolTipText("Send this authored destination to Shortest Path");
            path.addActionListener(event -> shortestPathBridge.pathTo(evaluation.getStep().getLocation()));
            row.add(path);
        }
        if (evaluation.getCompletion() == TruthValue.UNKNOWN
            && evaluation.getStep().getCompletion() != null
            && "MANUAL_ONLY".equalsIgnoreCase(evaluation.getStep().getCompletion().getType()))
        {
            JButton done = smallButton("DONE");
            done.setToolTipText("Confirm that this authored milestone is complete");
            done.addActionListener(event -> applyOverride(evaluation, ManualOverride.FORCE_COMPLETE));
            row.add(done);
        }
        JButton manage = smallButton("MANAGE");
        manage.setToolTipText("Manual completion, skip, and reset controls");
        manage.addActionListener(event -> showManageMenu(manage, evaluation));
        row.add(manage);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JPanel buildUpcoming(List<StepEvaluation> upcoming)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("NEXT ROUTE STEPS"));
        body.add(gap(5));
        int number = 2;
        for (StepEvaluation evaluation : upcoming)
        {
            JLabel line = labelHtml("<span style='color:#8f8f8f'>" + number++ + ".</span> "
                + escape(evaluation.getStep().getTitle()), statusColor(evaluation.getStatus()));
            line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            body.add(line);
        }
        return card(body);
    }

    private JPanel buildPreparation(List<PreparationEvaluation> preparation)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("PREP SOON"));
        body.add(gap(5));
        for (PreparationEvaluation item : preparation)
        {
            String detail;
            if (item.getStatus() == PreparationStatus.UNKNOWN)
            {
                detail = "bank not scanned yet";
            }
            else
            {
                detail = item.getActual() + " / " + item.getRequired();
            }
            Color color = item.getStatus() == PreparationStatus.KNOWN_PRESENT ? UiTokens.SUCCESS
                : item.getStatus() == PreparationStatus.KNOWN_MISSING ? UiTokens.DANGER : UiTokens.UNKNOWN;
            String icon = item.getStatus() == PreparationStatus.KNOWN_PRESENT ? "✓"
                : item.getStatus() == PreparationStatus.KNOWN_MISSING ? "×" : "?";
            body.add(labelHtml(icon + "  " + escape(item.getPreparation().getName())
                + " <span style='color:#8f8f8f'>" + escape(detail) + "</span>", color));
        }
        return card(body);
    }

    private JPanel buildWhileHere(List<WhileHereSpec> items)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("WHILE YOU'RE HERE"));
        body.add(gap(5));
        int shown = 0;
        for (WhileHereSpec item : items)
        {
            if (shown++ >= 3) break;
            body.add(labelHtml("<b>□ " + escape(item.getTitle()) + "</b><br>"
                + "<span style='color:#8f8f8f'>" + escape(item.getDetail()) + "</span>", UiTokens.TEXT));
            body.add(gap(5));
        }
        return card(body);
    }

    private JPanel buildFooter()
    {
        JPanel footer = verticalPanel();
        footer.setOpaque(false);
        JPanel row = new JPanel(new GridLayout(1, 2, 5, 0));
        row.setOpaque(false);
        JButton browser = smallButton("PATH");
        browser.addActionListener(event -> cards.show(cardHost, BROWSER));
        JButton gear = smallButton("GEAR");
        gear.addActionListener(event -> cards.show(cardHost, GEAR));
        row.add(browser);
        row.add(gear);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        footer.add(row);
        footer.add(gap(5));
        JButton refresh = smallButton("RE-EVALUATE");
        refresh.addActionListener(event -> reevaluate.run());
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, refresh.getPreferredSize().height));
        footer.add(refresh);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, footer.getPreferredSize().height));
        return footer;
    }

    private JPanel buildBrowser()
    {
        JPanel browser = new JPanel(new BorderLayout(0, 8));
        browser.setBackground(UiTokens.BACKGROUND);
        browser.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel top = new JPanel(new BorderLayout(5, 0));
        top.setOpaque(false);
        pathBack.addActionListener(event ->
        {
            if (routeDetailId == null) cards.show(cardHost, HOME);
            else { routeDetailId = null; rebuildBrowser(); }
        });
        search.setToolTipText("Search route steps and quests");
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { rebuildBrowser(); }
            @Override public void removeUpdate(DocumentEvent event) { rebuildBrowser(); }
            @Override public void changedUpdate(DocumentEvent event) { rebuildBrowser(); }
        });
        top.add(pathBack, BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        browser.add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(browserResults);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        browser.add(scroll, BorderLayout.CENTER);
        return browser;
    }

    private JScrollPane buildHomeScroll()
    {
        JScrollPane scroll = new JScrollPane(home);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTokens.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void rebuildBrowser()
    {
        if (browserResults == null)
        {
            return;
        }
        browserResults.removeAll();
        browserResults.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        if (projection == null)
        {
            browserResults.revalidate();
            return;
        }
        pathBack.setText(routeDetailId == null ? "OVERVIEW" : "BACK");
        if (routeDetailId != null)
        {
            StepEvaluation detail = findRouteStep(routeDetailId);
            if (detail != null) browserResults.add(buildCurrent(detail));
            browserResults.revalidate();
            browserResults.repaint();
            return;
        }
        String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
        RouteChapterProgress currentChapter = journey == null ? null : journey.getCurrent();
        if (query.isEmpty() && currentChapter != null)
        {
            JPanel chapter = verticalPanel();
            chapter.add(sectionLabel("CURRENT CHAPTER  ·  " + currentChapter.getCompleteCount()
                + "/" + currentChapter.getTotalCount()));
            chapter.add(gap(4));
            chapter.add(labelHtml("<b>" + escape(currentChapter.getChapter().getName()) + "</b>", UiTokens.ACCENT));
            chapter.add(gap(3));
            chapter.add(labelHtml(escape(currentChapter.getChapter().getDescription()), UiTokens.MUTED));
            browserResults.add(card(chapter));
            browserResults.add(gap(7));
        }
        int shown = 0;
        List<StepEvaluation> visibleSteps = query.isEmpty() && currentChapter != null
            ? currentChapter.getSteps() : projection.getSteps();
        for (StepEvaluation evaluation : visibleSteps)
        {
            String searchable = (evaluation.getStep().getTitle() + " " + evaluation.getStep().getCategory()).toLowerCase(Locale.ENGLISH);
            if (!query.isEmpty() && !searchable.contains(query))
            {
                continue;
            }
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setBackground(UiTokens.CARD);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
            JLabel status = new JLabel(statusGlyph(evaluation.getStatus()));
            status.setForeground(statusColor(evaluation.getStatus()));
            JLabel title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b><br>"
                + "<span style='color:#8f8f8f'>" + escape(humanize(evaluation.getStatus().name())) + "</span>",
                UiTokens.TEXT);
            row.add(status, BorderLayout.WEST);
            row.add(title, BorderLayout.CENTER);
            JButton view = smallButton("›");
            view.setToolTipText("Open step details");
            view.addActionListener(event ->
            {
                routeDetailId = evaluation.getStep().getId();
                rebuildBrowser();
            });
            row.add(view, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            browserResults.add(row);
            shown++;
        }
        if (shown == 0)
        {
            browserResults.add(labelHtml("No route steps match this search.", UiTokens.MUTED));
        }
        else if (query.isEmpty() && currentChapter != null)
        {
            int chapterIndex = journey.getChapters().indexOf(currentChapter);
            if (chapterIndex + 1 < journey.getChapters().size())
            {
                browserResults.add(gap(7));
                browserResults.add(card(labelHtml("<span style='color:#8f8f8f'>NEXT CHAPTER</span><br><b>○  "
                    + escape(journey.getChapters().get(chapterIndex + 1).getChapter().getName()) + "</b>", UiTokens.TEXT)));
            }
        }
        browserResults.revalidate();
        browserResults.repaint();
    }

    private StepEvaluation findRouteStep(String id)
    {
        if (projection == null) return null;
        for (StepEvaluation evaluation : projection.getSteps())
        {
            if (evaluation.getStep().getId().equals(id)) return evaluation;
        }
        return null;
    }

    private void showManageMenu(Component anchor, StepEvaluation evaluation)
    {
        JPopupMenu menu = new JPopupMenu();
        addMenuItem(menu, "Mark complete", () -> applyOverride(evaluation, ManualOverride.FORCE_COMPLETE));
        addMenuItem(menu, "Mark incomplete", () -> applyOverride(evaluation, ManualOverride.FORCE_INCOMPLETE));
        addMenuItem(menu, evaluation.getStatus() == StepStatus.SKIPPED_MANUALLY ? "Unskip" : "Skip",
            () -> applyOverride(evaluation, evaluation.getStatus() == StepStatus.SKIPPED_MANUALLY ? null : ManualOverride.SKIPPED));
        addMenuItem(menu, "Clear manual override", () -> applyOverride(evaluation, null));
        menu.addSeparator();
        addMenuItem(menu, "Reset all overrides…", this::confirmReset);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void addMenuItem(JPopupMenu menu, String title, Runnable action)
    {
        javax.swing.JMenuItem item = new javax.swing.JMenuItem(title);
        item.addActionListener(event -> action.run());
        menu.add(item);
    }

    private void applyOverride(StepEvaluation evaluation, ManualOverride override)
    {
        if (override == null)
        {
            persistence.remove(evaluation.getStep().getId());
        }
        else
        {
            persistence.put(evaluation.getStep().getId(), override);
        }
        reevaluate.run();
    }

    private void confirmReset()
    {
        int answer = JOptionPane.showConfirmDialog(this,
            "Reset every manual completion, skip, and incomplete override for this character?",
            "Reset IronPath overrides", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION)
        {
            persistence.clear();
            reevaluate.run();
        }
    }

    private void showIdle()
    {
        home.removeAll();
        home.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        home.add(sectionLabel("IRONPATH"));
        home.add(gap(8));
        home.add(card(labelHtml("<b>Your Ironman progression companion.</b><br><br>Log in to synchronize skills, quests, carried items, equipment, and the bank only after you open it.", UiTokens.TEXT)));
        home.revalidate();
        home.repaint();
    }

    private static JPanel notice(String text)
    {
        JPanel notice = card(labelHtml(escape(text), UiTokens.UNKNOWN));
        notice.setBackground(new Color(45, 40, 28));
        return notice;
    }

    private static JPanel card(Component content)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTokens.CARD);
        if (content instanceof JPanel)
        {
            content.setBackground(UiTokens.CARD);
        }
        panel.setBorder(UiTokens.cardBorder());
        panel.add(content, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTokens.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel scrollableVerticalPanel()
    {
        return new ScrollableVerticalPanel();
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(UiTokens.MUTED);
        label.setFont(UiTokens.LABEL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel labelHtml(String body, Color color)
    {
        JLabel label = new JLabel("<html><div style='width:140px'>" + body + "</div></html>");
        label.setForeground(color);
        label.setFont(UiTokens.BODY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel statusLine(TruthValue value, String text)
    {
        String icon = value == TruthValue.TRUE ? "✓" : value == TruthValue.FALSE ? "×" : "?";
        Color color = value == TruthValue.TRUE ? UiTokens.SUCCESS : value == TruthValue.FALSE ? UiTokens.DANGER : UiTokens.UNKNOWN;
        JLabel line = labelHtml(icon + "  " + escape(text), color);
        line.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        return line;
    }

    private static JButton smallButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setFont(UiTokens.LABEL);
        button.setMargin(new java.awt.Insets(4, 7, 4, 7));
        return button;
    }

    private static Component gap(int height)
    {
        Box.Filler filler = new Box.Filler(
            new Dimension(0, height), new Dimension(0, height), new Dimension(Integer.MAX_VALUE, height));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    private static Component verticalGlue()
    {
        Box.Filler filler = new Box.Filler(
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    private static String readinessTitle(StepEvaluation evaluation)
    {
        if (evaluation.getStatus() == StepStatus.BLOCKED || evaluation.getReadiness() == TruthValue.FALSE) return "BLOCKED";
        if (evaluation.getReadiness() == TruthValue.UNKNOWN) return "READINESS UNKNOWN";
        return "READY";
    }

    private static String priorityLabel(GearEvaluation evaluation)
    {
        if (evaluation.isSelectedGoal()) return "Selected goal";
        if (evaluation.getReadiness() == TruthValue.TRUE && evaluation.getUpgrade().getUsefulness() >= 5)
            return "High priority now";
        if (evaluation.getReadiness() == TruthValue.TRUE) return "Ready when you want it";
        if (evaluation.getStatus() == GearStatus.OPTIONAL) return "Optional detour";
        return "Build toward this";
    }

    private static String riskSuffix(StepEvaluation evaluation)
    {
        return evaluation.getStep().getRisk().name().equals("SAFE") ? "" : "  ·  " + humanize(evaluation.getStep().getRisk().name());
    }

    private static String detailSuffix(RequirementResult detail)
    {
        return detail.getDetail() == null || detail.getDetail().isEmpty() ? "" : " — " + detail.getDetail();
    }

    private static Color statusColor(StepStatus status)
    {
        switch (status)
        {
            case COMPLETE: return UiTokens.SUCCESS;
            case BLOCKED: return UiTokens.DANGER;
            case UNKNOWN: return UiTokens.UNKNOWN;
            case CURRENT: return UiTokens.ACCENT;
            default: return UiTokens.TEXT;
        }
    }

    private static String statusGlyph(StepStatus status)
    {
        switch (status)
        {
            case COMPLETE: return "✓";
            case CURRENT: return "→";
            case SKIPPED_MANUALLY: return "−";
            case BLOCKED: return "×";
            case UNKNOWN: return "?";
            default: return "○";
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

    private static final class ScrollableVerticalPanel extends JPanel implements Scrollable
    {
        private ScrollableVerticalPanel()
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(UiTokens.BACKGROUND);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
        {
            return Math.max(16, visibleRect.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }
}
