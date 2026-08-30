package com.ironcompass.ui;

import com.ironcompass.IronCompassConfig;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearPreferenceStore;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.IronCompassVersion;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalAction;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalRequirementResolver;
import com.ironcompass.goal.GoalResolution;
import com.ironcompass.integration.QuestHelperBridge;
import com.ironcompass.integration.ShortestPathBridge;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.AccountNeedEvaluation;
import com.ironcompass.planner.AccountNeedLevel;
import com.ironcompass.planner.AccountNeedService;
import com.ironcompass.planner.GoalInsightService;
import com.ironcompass.planner.GoalInsightsProjection;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.planner.PlannedAction;
import com.ironcompass.planner.PlannerPreferenceStore;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.planner.ProgressionCandidate;
import com.ironcompass.planner.RecommendationProjection;
import com.ironcompass.planner.ResourceReadiness;
import com.ironcompass.planner.SessionLength;
import com.ironcompass.planner.UnlockOpportunity;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.route.PreparationEvaluation;
import com.ironcompass.route.PreparationStatus;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RouteChapterProgress;
import com.ironcompass.route.RouteJourney;
import com.ironcompass.route.RouteJourneyService;
import com.ironcompass.route.StepEvaluation;
import com.ironcompass.route.StepStatus;
import com.ironcompass.route.StepType;
import com.ironcompass.route.WhileHereSpec;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.supply.SupplyForecast;
import com.ironcompass.supply.SupplyLine;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodDefinition;
import com.ironcompass.training.MethodPlannerService;
import com.ironcompass.training.MethodRecommendation;
import com.ironcompass.training.MethodResourceStatus;
import com.ironcompass.training.SkillTrainingPlan;
import com.ironcompass.training.TrainingPlanSegment;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

import static com.ironcompass.ui.UiComponents.*;

public final class IronCompassPanel extends PluginPanel
{
    private static final RouteJourneyService JOURNEY_SERVICE = new RouteJourneyService();
    private static final AccountNeedService ACCOUNT_NEEDS = new AccountNeedService();
    private static final GoalInsightService GOAL_INSIGHTS = new GoalInsightService(
        new com.ironcompass.requirement.ConditionEvaluator(), ACCOUNT_NEEDS);
    private static final String HOME = "home";
    private static final String BROWSER = "browser";
    private static final String GEAR = "gear";

    private final IronCompassConfig config;
    private final WikiBridge wikiBridge;
    private final ShortestPathBridge shortestPathBridge;
    private final QuestHelperBridge questHelperBridge;
    private final ManualOverrideStore persistence;
    private final GearPreferenceStore gearPreferences;
    private final PlannerPreferenceStore plannerPreferences;
    private final Runnable reevaluate;
    private final GoalPickerModel goalPicker;
    private final GoalCompletionService goalCompletion = new GoalCompletionService(
        new com.ironcompass.requirement.ConditionEvaluator());
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JButton overviewNav = navigationButton("OVERVIEW", "Open the account overview");
    private final JButton pathNav = navigationButton("PATH", "Browse the progression route");
    private final JButton gearNav = navigationButton("GEAR", "Browse gear objectives");
    private final JPanel home = scrollableVerticalPanel();
    private final JPanel browserResults = scrollableVerticalPanel();
    private final GearPathPanel gearPanel;
    private final JTextField search = textField("Search path...");
    private final JButton goalPickerButton = primaryButton("CHOOSE GOALS");
    private final JButton pathBack = ghostButton("BACK");
    private RouteProjection projection;
    private GearProjection gearProjection;
    private GoalResolution goalResolution;
    private SupplyForecast supplyForecast;
    private GoalPlanProjection goalPlan;
    private RecommendationProjection recommendations;
    private GoalInsightsProjection goalInsights;
    private RouteJourney journey;
    private AccountState accountState = AccountState.loggedOut();
    private String routeDetailId;
    private boolean showUsefulBreaks;
    private boolean showGoalDetails;
    private IronmanMethodCatalog methodCatalog;
    private MethodPlannerService methodPlanner;

    public IronCompassPanel(IronCompassConfig config, WikiBridge wikiBridge, ShortestPathBridge shortestPathBridge,
                         QuestHelperBridge questHelperBridge, ManualOverrideStore persistence, Runnable reevaluate)
    {
        this.config = config;
        this.wikiBridge = wikiBridge;
        this.shortestPathBridge = shortestPathBridge;
        this.questHelperBridge = questHelperBridge;
        this.persistence = persistence;
        this.gearPreferences = persistence instanceof GearPreferenceStore
            ? (GearPreferenceStore) persistence : new InMemoryGearPreferenceStore();
        this.plannerPreferences = persistence instanceof PlannerPreferenceStore
            ? (PlannerPreferenceStore) persistence : new InMemoryPlannerPreferenceStore();
        this.reevaluate = reevaluate;
        this.goalPicker = new GoalPickerModel(persistence);
        this.gearPanel = new GearPathPanel(wikiBridge, gearPreferences, persistence, reevaluate);

        setLayout(new BorderLayout());
        setBackground(UiTokens.BACKGROUND);
        add(buildNavigation(), BorderLayout.NORTH);
        cardHost.setBackground(UiTokens.BACKGROUND);
        cardHost.add(buildHomeScroll(), HOME);
        cardHost.add(buildBrowser(), BROWSER);
        cardHost.add(gearPanel, GEAR);
        add(cardHost, BorderLayout.CENTER);
        overviewNav.addActionListener(event -> showView(HOME));
        pathNav.addActionListener(event -> showView(BROWSER));
        gearNav.addActionListener(event -> showView(GEAR));
        goalPickerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        goalPickerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        goalPickerButton.getAccessibleContext().setAccessibleName("Open goal picker");
        goalPickerButton.getAccessibleContext().setAccessibleDescription(
            "Search and manage primary and secondary progression goals");
        goalPickerButton.addActionListener(event -> showGoalPicker());
        showView(HOME);
        showIdle();
    }

    public void setSkillPlanner(IronmanMethodCatalog catalog, MethodPlannerService planner)
    {
        methodCatalog = catalog;
        methodPlanner = planner;
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
        update(state, newProjection, newGearProjection, newGoalResolution, newSupplyForecast, null, null);
    }

    public void update(AccountState state, RouteProjection newProjection, GearProjection newGearProjection,
                       GoalResolution newGoalResolution, SupplyForecast newSupplyForecast,
                       GoalPlanProjection newGoalPlan, RecommendationProjection newRecommendations)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> update(state, newProjection, newGearProjection,
                newGoalResolution, newSupplyForecast, newGoalPlan, newRecommendations));
            return;
        }
        accountState = state;
        projection = newProjection;
        journey = newProjection == null ? null : JOURNEY_SERVICE.project(newProjection);
        gearProjection = newGearProjection;
        goalResolution = newGoalResolution;
        supplyForecast = newSupplyForecast;
        goalPlan = newGoalPlan;
        recommendations = newRecommendations;
        goalInsights = newGoalPlan == null || newGoalPlan.getCatalog() == null ? null
            : GOAL_INSIGHTS.evaluate(newGoalPlan.getCatalog(), state, newGearProjection, newGoalPlan, persistence);
        updateGoalPickerButton();
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
        home.add(sectionLabel("IRON COMPASS"));
        home.add(gap(8));
        home.add(card(labelHtml("<b>Route data could not be loaded.</b><br><br>" + escape(message), UiTokens.DANGER)));
        home.revalidate();
        home.repaint();
        showView(HOME);
    }

    void showPathForTesting()
    {
        showView(BROWSER);
    }

    void showPathSearchForTesting(String query)
    {
        search.setText(query);
        showView(BROWSER);
    }

    void showCurrentPathDetailForTesting()
    {
        if (projection == null || projection.getSteps().isEmpty()) return;
        StepEvaluation selected = projection.getCurrent();
        if (selected == null) selected = projection.getSteps().get(0);
        routeDetailId = selected.getStep().getId();
        rebuildBrowser();
        showView(BROWSER);
    }

    void showGearForTesting(String style)
    {
        gearPanel.selectStyleForTesting(style);
        showView(GEAR);
    }

    void showGearSearchForTesting(String query)
    {
        gearPanel.setSearchForTesting(query);
        showView(GEAR);
    }

    void showGearObjectiveForTesting(String objectiveId)
    {
        gearPanel.showObjective(objectiveId);
        showView(GEAR);
    }

    void showUsefulBreaksForTesting()
    {
        showUsefulBreaks = true;
        rebuildHome();
    }

    JPanel goalPickerContentForTesting()
    {
        if (goalPlan == null || goalPlan.getCatalog() == null) return new JPanel();
        return new GoalPickerDialog(this, goalPicker, goalPlan, accountState, gearProjection, projection,
            gearPreferences, persistence, goalCompletion, reevaluate).contentForTesting();
    }

    JPanel accountInsightsContentForTesting()
    {
        if (goalInsights == null) return new JPanel();
        return new AccountInsightsDialog(this, goalInsights).contentForTesting();
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
            home.add(notice("Iron Compass is tuned for Ironman progression. Route preview is available, but this account is not an Ironman."));
        }
        else if (accountState.getAccountMode().isUltimate())
        {
            home.add(gap(8));
            home.add(notice("Ultimate Ironman detected. This route is not inventory-optimized for UIM; use it as a quest-order preview."));
        }

        home.add(gap(10));
        home.add(buildPosition());
        home.add(gap(10));

        if (goalPlan != null)
        {
            home.add(buildGoalPlanner());
            home.add(gap(10));
        }
        if (goalInsights != null)
        {
            home.add(buildGoalInsights());
            home.add(gap(10));
        }
        if (recommendations != null && recommendations.getNewOpportunity() != null)
        {
            home.add(buildNewOpportunity(recommendations.getNewOpportunity()));
            home.add(gap(10));
        }

        GoalAction goalAction = goalResolution == null ? null : goalResolution.getNextAction();
        if (hasRecommendations())
        {
            addRecommendations();
        }
        else if (goalAction != null && goalAction.getRouteStep() != null)
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

        if (!hasRecommendations() && gearProjection != null && gearProjection.getRecommended() != null)
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

    private boolean hasRecommendations()
    {
        return recommendations != null && (recommendations.getRecommended() != null
            || recommendations.getQuickWin() != null || recommendations.getLongTerm() != null);
    }

    private void addRecommendations()
    {
        boolean primaryAlreadyShown = recommendationMatchesPrimaryAction(recommendations.getRecommended());
        if (recommendations.getRecommended() != null && !primaryAlreadyShown)
        {
            home.add(buildCandidate("RECOMMENDED", recommendations.getRecommended()));
        }
        if (recommendations.getQuickWin() != null)
        {
            if (recommendations.getRecommended() != null && !primaryAlreadyShown) home.add(gap(10));
            home.add(buildCandidate("QUICK WIN", recommendations.getQuickWin()));
        }
        if (recommendations.getLongTerm() != null)
        {
            if ((recommendations.getRecommended() != null && !primaryAlreadyShown)
                || recommendations.getQuickWin() != null) home.add(gap(10));
            home.add(buildCandidate("LONG-TERM", recommendations.getLongTerm()));
        }
        if (!recommendations.getUsefulBreaks().isEmpty())
        {
            home.add(gap(8));
            JButton alternatives = ghostButton(showUsefulBreaks ? "HIDE OTHER PROGRESS" : "TAKE A USEFUL BREAK");
            alternatives.setAlignmentX(Component.LEFT_ALIGNMENT);
            alternatives.setToolTipText("Show other actions that still advance this account");
            alternatives.addActionListener(event ->
            {
                showUsefulBreaks = !showUsefulBreaks;
                rebuildHome();
            });
            home.add(alternatives);
            if (showUsefulBreaks)
            {
                home.add(gap(7));
                home.add(buildUsefulBreaks(recommendations.getUsefulBreaks()));
            }
        }
    }

    private boolean recommendationMatchesPrimaryAction(ProgressionCandidate candidate)
    {
        return candidate != null && goalPlan != null && goalPlan.getNextAction() != null
            && candidate.getId().equals(goalPlan.getNextAction().stableKey());
    }

    private JPanel buildGoalPlanner()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("ACTIVE GOALS"));
        body.add(gap(UiTokens.SM));
        if (goalPlan.hasSelectedGoal())
        {
            body.add(badge("PRIMARY", UiTokens.ACCENT));
            body.add(gap(UiTokens.XS));
            body.add(labelHtml("<b>" + escape(goalPlan.getTitle()) + "</b>", UiTokens.ACCENT_HOVER));
            if (goalPlan.getGoal() != null && (goalPlan.getGoal().isRng()
                || goalPlan.getGoal().getCompletionMode() != com.ironcompass.goal.GoalCompletionMode.AUTO))
            {
                JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.XS, 0));
                flags.setOpaque(false);
                flags.setAlignmentX(Component.LEFT_ALIGNMENT);
                if (goalPlan.getGoal().isRng()) flags.add(badge("RNG", UiTokens.WARNING));
                if (goalPlan.getGoal().getCompletionMode() != com.ironcompass.goal.GoalCompletionMode.AUTO)
                    flags.add(badge("MANUAL", UiTokens.UNKNOWN));
                body.add(flags);
            }
            long blockers = goalPlan.getProgress().stream()
                .filter(value -> value.getValue() == TruthValue.FALSE).count();
            long unknown = goalPlan.getProgress().stream()
                .filter(value -> value.getValue() == TruthValue.UNKNOWN).count();
            if (blockers > 0 || unknown > 0)
                body.add(labelHtml(blockers + " blocker(s)" + (unknown > 0 ? " · " + unknown + " unknown" : ""),
                    UiTokens.TEXT_MUTED));
        }
        else
            body.add(labelHtml("No primary goal selected.", UiTokens.MUTED));
        if (!goalPlan.getSecondaryGoals().isEmpty())
        {
            body.add(gap(UiTokens.SM));
            body.add(sectionLabel("SECONDARY"));
            for (GoalPlanProjection secondary : goalPlan.getSecondaryGoals())
                body.add(new WrappingText("○  " + escape(secondary.getTitle()),
                    UiTokens.TEXT_SECONDARY, UiTokens.META));
        }
        body.add(gap(UiTokens.MD));
        body.add(goalPickerButton);
        if (goalPlan.getUnavailableSelectedId() != null)
        {
            body.add(gap(6));
            body.add(statusLine(TruthValue.UNKNOWN,
                "The saved goal cannot be pursued by this version. Choose another goal or clear it."));
        }
        else if (goalPlan.hasSelectedGoal())
        {
            PlannedAction action = goalPlan.getNextAction();
            if (action != null)
            {
                body.add(gap(UiTokens.LG));
                body.add(sectionLabel("NEXT BEST MOVE"));
                body.add(gap(UiTokens.XS));
                body.add(labelHtml("<b>" + escape(action.getTitle()) + "</b>",
                    action.getKind() == PlannedAction.Kind.COMPLETE ? UiTokens.SUCCESS : UiTokens.ACCENT_HOVER));
                body.add(gap(UiTokens.SM));
                body.add(sectionLabel("WHY THIS?"));
                body.add(gap(UiTokens.XS));
                ProgressionCandidate ranked = recommendations == null ? null : recommendations.getRecommended();
                if (recommendationMatchesPrimaryAction(ranked))
                {
                    int shown = 0;
                    for (String why : ranked.getWhyLines())
                    {
                        body.add(labelHtml("•  " + escape(why), UiTokens.TEXT_SECONDARY));
                        if (++shown == 1) break;
                    }
                }
                else
                    body.add(labelHtml(escape(goalPlan.getWhyNow()), UiTokens.MUTED));
                addMethodRecommendation(body, showGoalDetails);
            }
            addGoalProgress(body);
            if (showGoalDetails && goalPlan.getAfterThis() != null)
            {
                body.add(gap(7));
                body.add(sectionLabel("AFTER THIS"));
                body.add(gap(3));
                body.add(labelHtml(escape(goalPlan.getAfterThis()), UiTokens.TEXT));
            }
            if (showGoalDetails)
            {
                body.add(gap(UiTokens.MD));
                body.add(sectionLabel("UNLOCKS"));
                body.add(gap(UiTokens.XS));
                int shown = 0;
                for (String unlock : goalPlan.getUnlocks())
                {
                    body.add(labelHtml("○  " + escape(unlock), UiTokens.TEXT));
                    if (++shown == 1) break;
                }
                if (goalPlan.getUnlocks().size() > shown)
                    body.add(labelHtml("+ " + (goalPlan.getUnlocks().size() - shown)
                        + " more unlock(s)", UiTokens.MUTED));
            }
            ResourceReadiness resources = goalPlan.getResourceReadiness();
            if (resources != null && (showGoalDetails || resources.getValue() != TruthValue.TRUE))
            {
                body.add(gap(7));
                body.add(sectionLabel("RESOURCE READINESS"));
                body.add(gap(3));
                body.add(statusLine(resources.getValue(), resources.getSummary()));
            }
            body.add(gap(UiTokens.SM));
            JButton disclosure = ghostButton(showGoalDetails ? "LESS DETAIL" : "MORE DETAILS");
            disclosure.setToolTipText("Show or hide supporting goal information");
            disclosure.addActionListener(event ->
            {
                showGoalDetails = !showGoalDetails;
                rebuildHome();
            });
            body.add(disclosure);
        }
        body.add(gap(8));
        body.add(goalPlannerActions());
        body.add(gap(3));
        body.add(labelHtml(humanize(plannerPreferences.getPlaystyle().name()) + " · "
            + plannerPreferences.getSessionLength().getLabel()
            + (plannerPreferences.isAvoidWilderness() ? " · Avoid Wilderness" : ""), UiTokens.MUTED));
        return card(body, CardStyle.HERO);
    }

    private JPanel buildGoalInsights()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("ACCOUNT HEALTH"));
        body.add(gap(UiTokens.SM));
        List<AccountNeedEvaluation> health = new java.util.ArrayList<>(goalInsights.getHealth().getEvaluations());
        health.sort(java.util.Comparator.comparingInt(IronCompassPanel::healthSummaryOrder));
        int shown = 0;
        for (AccountNeedEvaluation value : health)
        {
            body.add(healthRow(value));
            if (++shown == 4) break;
        }
        if (!accountState.getBank().isObserved())
        {
            body.add(gap(UiTokens.SM));
            body.add(labelHtml("<b>BANK UNKNOWN</b> · reserves are not treated as empty", UiTokens.WARNING));
        }

        JButton details = smallButton("VIEW ACCOUNT INSIGHTS");
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.addActionListener(event -> showGoalInsights());
        body.add(gap(8));
        body.add(details);
        return card(body, CardStyle.SUBTLE);
    }

    private JPanel healthRow(AccountNeedEvaluation evaluation)
    {
        JPanel row = new JPanel(new BorderLayout(UiTokens.SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel("•  " + compactHealthLabel(evaluation));
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

    private static Color healthColor(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.STRONG) return UiTokens.SUCCESS.brighter();
        if (level == AccountNeedLevel.GOOD) return UiTokens.SUCCESS;
        if (level == AccountNeedLevel.DEVELOPING) return UiTokens.WARNING;
        if (level == AccountNeedLevel.WEAK) return new Color(178, 126, 91);
        return UiTokens.TEXT_MUTED;
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

    private void showGoalInsights()
    {
        if (goalInsights == null) return;
        new AccountInsightsDialog(this, goalInsights).showDialog();
    }

    private static TruthValue healthTruth(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.GOOD || level == AccountNeedLevel.STRONG) return TruthValue.TRUE;
        if (level == AccountNeedLevel.WEAK) return TruthValue.FALSE;
        return TruthValue.UNKNOWN;
    }

    private static int healthRank(AccountNeedLevel level)
    {
        if (level == AccountNeedLevel.WEAK) return 0;
        if (level == AccountNeedLevel.DEVELOPING) return 1;
        if (level == AccountNeedLevel.UNKNOWN) return 2;
        if (level == AccountNeedLevel.GOOD) return 3;
        return 4;
    }

    private void addGoalProgress(JPanel body)
    {
        if (goalPlan.getProgress().isEmpty()) return;
        body.add(gap(7));
        body.add(sectionLabel("PROGRESS"));
        body.add(gap(3));
        List<RequirementResult> ordered = new java.util.ArrayList<>(goalPlan.getProgress());
        ordered.sort(java.util.Comparator.comparingInt(result -> result.getValue() == TruthValue.TRUE ? 1 : 0));
        int shown = 0;
        for (RequirementResult result : ordered)
        {
            body.add(statusLine(result.getValue(), result.getLabel() + detailSuffix(result)));
            if (++shown == 3) break;
        }
        if (ordered.size() > shown)
            body.add(labelHtml("+ " + (ordered.size() - shown) + " more requirement(s)", UiTokens.MUTED));
    }

    private JPanel goalPlannerActions()
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        SkillTrainingPlan training = goalPlan.getSkillTrainingPlan();
        if (training != null)
        {
            JButton skillPlan = primaryButton("VIEW SKILL PLAN");
            skillPlan.setToolTipText("Open the account-aware training path for this skill target");
            skillPlan.addActionListener(event -> showSkillPlanner(training.getSkill(),
                training.getTargetLevel(), false));
            row.add(skillPlan);
        }
        if (goalPlan.hasSelectedGoal() && goalPlan.getWikiPage() != null)
        {
            JButton wiki = training == null ? primaryButton("OPEN WIKI") : smallButton("WIKI");
            wiki.addActionListener(event -> wikiBridge.open(goalPlan.getWikiPage()));
            row.add(wiki);
        }
        if (goalPlan.hasSelectedGoal() || goalPlan.getUnavailableSelectedId() != null)
        {
            JButton clear = ghostButton("CLEAR");
            clear.setToolTipText("Clear the primary progression goal");
            clear.getAccessibleContext().setAccessibleName("Clear primary goal");
            clear.addActionListener(event ->
            {
                gearPreferences.setPrimaryGoalId(null);
                reevaluate.run();
            });
            row.add(clear);
        }
        JButton preferences = ghostButton(goalPlan.hasSelectedGoal() ? "PREFS" : "PREFERENCES");
        preferences.setToolTipText("Change playstyle, Wilderness, and session ranking preferences");
        preferences.getAccessibleContext().setAccessibleName("Planner preferences");
        preferences.addActionListener(event -> showPlannerPreferences(preferences));
        row.add(preferences);
        if (training == null && methodCatalog != null)
        {
            JButton skills = ghostButton("SKILLS");
            skills.setToolTipText("Open the Ironman Skill Planner");
            skills.addActionListener(event -> showSkillPlanner(defaultSkill(), 99, false));
            row.add(skills);
        }
        return row;
    }

    private JPanel buildCandidate(String label, ProgressionCandidate candidate)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(label));
        body.add(gap(4));
        body.add(labelHtml("<b>" + escape(candidate.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(humanize(candidate.getImpact()) + " · "
            + humanize(candidate.getEffort().name()) + " effort", UiTokens.MUTED));
        body.add(gap(5));
        body.add(sectionLabel("WHY THIS?"));
        body.add(gap(2));
        int reasonCount = 0;
        for (String why : candidate.getWhyLines())
        {
            body.add(labelHtml("•  " + escape(why), UiTokens.TEXT_SECONDARY));
            if (++reasonCount == 1) break;
        }
        if (candidate.getActiveGoalCount() > 1)
            body.add(labelHtml("Goals: " + escape(String.join(", ", candidate.getAdvancedGoals())), UiTokens.MUTED));
        else if (candidate.getUnlockSummary() != null && !candidate.getUnlockSummary().equals(candidate.getReason()))
        {
            body.add(labelHtml("Unlocks: " + escape(candidate.getUnlockSummary()), UiTokens.MUTED));
        }
        JButton open = candidateAction(candidate);
        if (open != null)
        {
            body.add(gap(7));
            body.add(open);
        }
        CardStyle style = "RECOMMENDED".equals(label) ? CardStyle.HERO
            : "QUICK WIN".equals(label) ? CardStyle.SUCCESS : CardStyle.SUBTLE;
        return card(body, style);
    }

    private void showSkillPlanner(String skill, int target, boolean fullGuide)
    {
        if (methodCatalog == null || methodPlanner == null) return;
        List<GoalPlanProjection> active = goalPlan == null
            ? java.util.Collections.emptyList() : goalPlan.getActiveGoals();
        new SkillPlannerDialog(this, methodCatalog, methodPlanner, accountState, plannerPreferences, active,
            wikiBridge, skill, target, fullGuide).showDialog();
    }

    private String defaultSkill()
    {
        if (goalPlan != null && goalPlan.getNextAction() != null
            && goalPlan.getNextAction().getSkill() != null
            && methodCatalog.hasFullGuide(goalPlan.getNextAction().getSkill()))
            return goalPlan.getNextAction().getSkill();
        return methodCatalog == null || methodCatalog.getFullGuideSkills().isEmpty()
            ? "Hunter" : methodCatalog.getFullGuideSkills().get(0);
    }

    private void addMethodRecommendation(JPanel body, boolean expanded)
    {
        MethodRecommendation recommendation = goalPlan.getMethodRecommendation();
        if (recommendation == null || recommendation.getRecommended() == null) return;
        IronmanMethodDefinition method = recommendation.getRecommended();
        MethodResourceStatus resource = recommendation.getResourceStatus();
        SkillTrainingPlan plan = goalPlan.getSkillTrainingPlan();
        TrainingPlanSegment first = plan == null || plan.getSegments().isEmpty() ? null : plan.getSegments().get(0);
        body.add(gap(UiTokens.MD));
        body.add(sectionLabel("RECOMMENDED METHOD"));
        body.add(gap(UiTokens.XS));
        body.add(labelHtml("<b>" + escape(method.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml((first == null ? "" : first.getFromLevel() + " → " + first.getToLevel() + " · ")
            + escape(recommendation.getXpRateSummary()), UiTokens.MUTED));
        if (expanded)
        {
            body.add(labelHtml(escape(method.getDescription()), UiTokens.TEXT));
            body.add(labelHtml(escape(recommendation.getReason()), UiTokens.MUTED));
            if (recommendation.getRequirementStatus() == TruthValue.UNKNOWN)
                body.add(statusLine(TruthValue.UNKNOWN, "Method access is not fully confirmed."));
        }
        boolean criticalResource = resource == MethodResourceStatus.UNKNOWN
            || resource == MethodResourceStatus.EMPTY || resource == MethodResourceStatus.PARTIAL;
        if (resource != MethodResourceStatus.NOT_APPLICABLE && (expanded || criticalResource))
        {
            TruthValue value = resource == MethodResourceStatus.SUFFICIENT ? TruthValue.TRUE
                : resource == MethodResourceStatus.EMPTY ? TruthValue.FALSE : TruthValue.UNKNOWN;
            body.add(gap(UiTokens.SM));
            body.add(sectionLabel("RESOURCE READINESS"));
            body.add(statusLine(value, recommendation.getResourceSummary()));
            if (expanded && (resource == MethodResourceStatus.EMPTY || resource == MethodResourceStatus.PARTIAL)
                && !method.getAcquisitionSources().isEmpty())
            {
                body.add(gap(4));
                body.add(sectionLabel("USEFUL SOURCES"));
                int shown = 0;
                for (String source : method.getAcquisitionSources())
                {
                    body.add(labelHtml("○  " + escape(source), UiTokens.TEXT));
                    if (++shown == 3) break;
                }
            }
        }
        if (expanded && !recommendation.getAlternatives().isEmpty())
            body.add(labelHtml("Alternative: " + escape(recommendation.getAlternatives().get(0).getTitle()),
                UiTokens.MUTED));
        if (expanded && !recommendation.getLockedAlternatives().isEmpty())
            body.add(labelHtml("Locked option: "
                + escape(recommendation.getLockedAlternatives().get(0).getTitle()), UiTokens.UNKNOWN));
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

    private JButton candidateAction(ProgressionCandidate candidate)
    {
        if (candidate.getRouteStep() != null)
        {
            JButton open = smallButton("VIEW IN PATH");
            open.setAlignmentX(Component.LEFT_ALIGNMENT);
            open.addActionListener(event ->
            {
                routeDetailId = candidate.getRouteStep().getStep().getId();
                rebuildBrowser();
                showView(BROWSER);
            });
            return open;
        }
        if (candidate.getGearStep() != null)
        {
            JButton open = smallButton("VIEW IN GEAR");
            open.setAlignmentX(Component.LEFT_ALIGNMENT);
            open.addActionListener(event ->
            {
                gearPanel.showObjective(candidate.getGearStep().getUpgrade().getId());
                showView(GEAR);
            });
            return open;
        }
        if (candidate.getGoal() != null && methodCatalog != null && methodPlanner != null)
        {
            ConditionSpec target = skillTarget(candidate.getGoal().getCompletion());
            if (target != null && methodCatalog.hasFullGuide(target.getSkill())
                && accountState.skillLevel(target.getSkill()) < target.getLevel())
            {
                JButton open = smallButton("VIEW SKILL PLAN");
                open.setAlignmentX(Component.LEFT_ALIGNMENT);
                open.addActionListener(event -> showSkillPlanner(target.getSkill(), target.getLevel(), false));
                return open;
            }
        }
        return null;
    }

    private JPanel buildNewOpportunity(UnlockOpportunity opportunity)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("NEW OPPORTUNITY"));
        body.add(gap(4));
        body.add(labelHtml("<b>" + escape(opportunity.getTitle()) + "</b>", UiTokens.SUCCESS));
        body.add(gap(3));
        body.add(labelHtml(escape(opportunity.getExplanation()), UiTokens.TEXT));
        return card(body, CardStyle.SUCCESS);
    }

    private JPanel buildHeader()
    {
        JPanel panel = verticalPanel();
        panel.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        if (compassIcon() != null)
        {
            JLabel icon = new JLabel(compassIcon());
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UiTokens.SM));
            icon.getAccessibleContext().setAccessibleName("Iron Compass logo");
            brand.add(icon);
        }
        JLabel title = new JLabel("IRON COMPASS");
        title.setForeground(UiTokens.ACCENT);
        title.setFont(UiTokens.APP_TITLE);
        brand.add(title);
        JLabel percentage = new JLabel(String.format(Locale.ENGLISH, "%.0f%%", projection.getProgressPercent()));
        percentage.setForeground(UiTokens.TEXT_PRIMARY);
        percentage.setFont(UiTokens.META.deriveFont(java.awt.Font.BOLD));
        titleRow.add(brand, BorderLayout.WEST);
        titleRow.add(percentage, BorderLayout.EAST);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleRow.getPreferredSize().height));
        panel.add(titleRow);

        RouteChapterProgress chapter = journey == null ? null : journey.getCurrent();
        String context = chapter == null ? projection.getRoute().getName()
            : chapter.getChapter().getName() + " " + chapter.getCompleteCount() + "/" + chapter.getTotalCount();
        WrappingText route = labelHtml(escape(context) + "<br>Overall " + projection.getCompleteCount()
            + "/" + projection.getTotalCount() + "  ·  " + escape(humanize(accountState.getAccountMode().name())),
            UiTokens.MUTED);
        panel.add(route);
        panel.add(gap(UiTokens.SM));
        JProgressBar progress = new JProgressBar(0, Math.max(1, projection.getTotalCount()));
        progress.setValue(projection.getCompleteCount());
        progress.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 6));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progress.setForeground(UiTokens.ACCENT);
        progress.setBackground(UiTokens.SURFACE);
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
            showView(GEAR);
        });
        body.add(gear);
        return card(body, CardStyle.HERO);
    }

    private JPanel buildGoalRouteStep(GoalAction action)
    {
        StepEvaluation evaluation = action.getRouteStep();
        JPanel body = verticalPanel();
        body.add(sectionLabel("DO THIS NOW"));
        body.add(gap(6));
        WrappingText title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b>", UiTokens.ACCENT);
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
        return card(body, CardStyle.HERO);
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
            showView(GEAR);
        });
        body.add(gear);
        return card(body, CardStyle.HERO);
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
        JButton doNow = evaluation.isSelectedGoal() ? smallButton("GOAL SET") : primaryButton("DO THIS NOW");
        doNow.addActionListener(event ->
        {
            gearPreferences.setSelectedGoalId(evaluation.getUpgrade().getId());
            reevaluate.run();
        });
        JButton gear = smallButton("VIEW");
        gear.addActionListener(event ->
        {
            gearPanel.showObjective(evaluation.getUpgrade().getId());
            showView(GEAR);
        });
        actions.add(doNow);
        actions.add(gear);
        body.add(actions);
        return card(body, CardStyle.HERO);
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
                    || evaluation.getStatus() == GearStatus.UNCONFIRMED
                    || evaluation.getStatus() == GearStatus.SKIPPED)
                {
                    continue;
                }
                body.add(labelHtml("○  " + escape(evaluation.getUpgrade().getName()), UiTokens.TEXT));
                if (++shown == 3) break;
            }
            if (shown == 0) body.add(labelHtml("Choose any Gear objective to build a dependency path.", UiTokens.MUTED));
        }
        return card(body, CardStyle.SUBTLE);
    }

    private JPanel buildCurrent(StepEvaluation evaluation)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel(evaluation.getStatus() == StepStatus.CURRENT ? "DO THIS NOW"
            : "STEP DETAIL  ·  " + humanize(evaluation.getStatus().name()).toUpperCase(Locale.ENGLISH)));
        body.add(gap(6));
        WrappingText title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b>", UiTokens.ACCENT);
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
        return card(body, CardStyle.HERO);
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
        if (methodCatalog == null || methodPlanner == null || evaluation.getStep().getType() != StepType.TRAIN)
            return;
        ConditionSpec target = skillTarget(evaluation.getStep().getCompletion());
        if (target == null || !methodCatalog.hasFullGuide(target.getSkill())) return;
        List<GoalPlanProjection> active = goalPlan == null
            ? java.util.Collections.emptyList() : goalPlan.getActiveGoals();
        SkillTrainingPlan plan = methodPlanner.plan(methodCatalog, target.getSkill(), target.getLevel(),
            accountState, plannerPreferences, active);
        if (plan == null || plan.getFirstRecommendation() == null) return;
        MethodRecommendation recommendation = plan.getFirstRecommendation();
        IronmanMethodDefinition method = recommendation.getRecommended();
        TrainingPlanSegment first = plan.getSegments().get(0);
        body.add(gap(8));
        body.add(sectionLabel("TRAINING METHOD"));
        body.add(gap(3));
        body.add(labelHtml("<b>" + escape(method.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(first.getFromLevel() + " → " + first.getToLevel() + " · "
            + escape(recommendation.getXpRateSummary()), UiTokens.MUTED));
        body.add(labelHtml(escape(recommendation.getReason()), UiTokens.TEXT_SECONDARY));
        MethodResourceStatus resource = recommendation.getResourceStatus();
        if (resource == MethodResourceStatus.UNKNOWN || resource == MethodResourceStatus.EMPTY
            || resource == MethodResourceStatus.PARTIAL)
            body.add(statusLine(resource == MethodResourceStatus.EMPTY ? TruthValue.FALSE : TruthValue.UNKNOWN,
                recommendation.getResourceSummary()));
        JButton view = smallButton("VIEW TRAINING PLAN");
        view.setAlignmentX(Component.LEFT_ALIGNMENT);
        view.addActionListener(event -> showSkillPlanner(plan.getSkill(), plan.getTargetLevel(), false));
        body.add(gap(UiTokens.SM));
        body.add(view);
    }

    private static ConditionSpec skillTarget(ConditionSpec condition)
    {
        if (condition == null) return null;
        if ("SKILL_AT_LEAST".equalsIgnoreCase(condition.getType())) return condition;
        for (ConditionSpec child : condition.getChildren())
        {
            ConditionSpec target = skillTarget(child);
            if (target != null) return target;
        }
        return skillTarget(condition.getChild());
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
            JButton done = primaryButton("DONE");
            done.setToolTipText("Confirm that this authored milestone is complete");
            done.addActionListener(event -> applyOverride(evaluation, ManualOverride.FORCE_COMPLETE));
            row.add(done);
        }
        JButton manage = ghostButton("MANAGE");
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
            WrappingText line = labelHtml("<span style='color:#8f8f8f'>" + number++ + ".</span> "
                + escape(evaluation.getStep().getTitle()), statusColor(evaluation.getStatus()));
            line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            body.add(line);
        }
        return card(body, CardStyle.SUBTLE);
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
        return card(body, CardStyle.WARNING);
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
        return card(body, CardStyle.SUBTLE);
    }

    private JPanel buildFooter()
    {
        JPanel footer = verticalPanel();
        footer.setOpaque(false);
        JButton refresh = ghostButton("RE-EVALUATE");
        refresh.addActionListener(event -> reevaluate.run());
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, refresh.getPreferredSize().height));
        footer.add(refresh);
        footer.add(gap(UiTokens.XS));
        footer.add(labelHtml("Iron Compass v" + escape(IronCompassVersion.get()), UiTokens.TEXT_MUTED));
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
            routeDetailId = null;
            rebuildBrowser();
        });
        search.setToolTipText("Search route steps and quests");
        search.getAccessibleContext().setAccessibleName("Search progression route");
        search.getAccessibleContext().setAccessibleDescription("Filter route steps by title or category");
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { rebuildBrowser(); }
            @Override public void removeUpdate(DocumentEvent event) { rebuildBrowser(); }
            @Override public void changedUpdate(DocumentEvent event) { rebuildBrowser(); }
        });
        top.add(pathBack, BorderLayout.WEST);
        JPanel searchBox = verticalPanel();
        searchBox.add(sectionLabel("SEARCH PATH"));
        searchBox.add(gap(2));
        searchBox.add(search);
        top.add(searchBox, BorderLayout.CENTER);
        browser.add(top, BorderLayout.NORTH);
        JScrollPane scroll = scrollPane(browserResults);
        browser.add(scroll, BorderLayout.CENTER);
        return browser;
    }

    private JScrollPane buildHomeScroll()
    {
        JScrollPane scroll = scrollPane(home);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
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
        pathBack.setVisible(routeDetailId != null);
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
            browserResults.add(card(chapter, CardStyle.HERO));
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
            boolean current = evaluation.getStatus() == StepStatus.CURRENT;
            Color rowColor = current ? UiTokens.SURFACE_SELECTED : UiTokens.SURFACE;
            row.setBackground(rowColor);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, current ? 2 : 0, 1, 0,
                    current ? UiTokens.ACCENT : UiTokens.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
            JLabel status = new JLabel(statusGlyph(evaluation.getStatus()));
            status.setForeground(statusColor(evaluation.getStatus()));
            WrappingText title = labelHtml("<b>" + escape(evaluation.getStep().getTitle()) + "</b><br>"
                + "<span style='color:#8f8f8f'>" + escape(humanize(evaluation.getStatus().name())) + "</span>",
                current ? UiTokens.ACCENT_HOVER
                    : evaluation.getStatus() == StepStatus.COMPLETE ? UiTokens.TEXT_SECONDARY : UiTokens.TEXT_PRIMARY);
            row.add(status, BorderLayout.WEST);
            row.add(title, BorderLayout.CENTER);
            JButton view = iconButton("›", "Open step details");
            view.setToolTipText("Open step details");
            view.addActionListener(event ->
            {
                routeDetailId = evaluation.getStep().getId();
                rebuildBrowser();
            });
            row.add(view, BorderLayout.EAST);
            installHover(row, rowColor);
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
                    + escape(journey.getChapters().get(chapterIndex + 1).getChapter().getName()) + "</b>", UiTokens.TEXT),
                    CardStyle.SUBTLE));
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
        stylePopupMenu(menu);
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
        javax.swing.JMenuItem item = styleMenuItem(new javax.swing.JMenuItem(title));
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
            "Reset Iron Compass overrides", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
        home.add(sectionLabel("IRON COMPASS  ·  v" + IronCompassVersion.get()));
        home.add(gap(8));
        home.add(card(labelHtml("<b>Your Ironman progression companion.</b><br><br>Log in to synchronize skills, quests, carried items, equipment, and the bank only after you open it.", UiTokens.TEXT)));
        home.revalidate();
        home.repaint();
    }

    private JPanel buildNavigation()
    {
        JPanel navigation = new JPanel(new GridLayout(1, 3, 4, 0));
        navigation.setBackground(UiTokens.BACKGROUND);
        navigation.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER),
            BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        navigation.add(overviewNav);
        navigation.add(pathNav);
        navigation.add(gearNav);
        return navigation;
    }

    private void updateGoalPickerButton()
    {
        int active = goalPlan == null ? 0 : goalPlan.getActiveGoals().size();
        goalPickerButton.setText(active == 0 ? "CHOOSE GOALS" : "MANAGE GOALS  ·  " + active);
        goalPickerButton.putClientProperty(PremiumButtonUI.STYLE_PROPERTY,
            active == 0 ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY);
        goalPickerButton.repaint();
    }

    private void showGoalPicker()
    {
        if (goalPlan == null || goalPlan.getCatalog() == null) return;
        new GoalPickerDialog(this, goalPicker, goalPlan, accountState, gearProjection, projection,
            gearPreferences, persistence, goalCompletion, reevaluate).showDialog();
    }

    private void showPlannerPreferences(Component anchor)
    {
        JPopupMenu menu = new JPopupMenu();
        stylePopupMenu(menu);
        ButtonGroup styles = new ButtonGroup();
        for (Playstyle style : Playstyle.values())
        {
            JRadioButtonMenuItem item = styleMenuItem(new JRadioButtonMenuItem(humanize(style.name()),
                plannerPreferences.getPlaystyle() == style));
            item.addActionListener(event ->
            {
                plannerPreferences.setPlaystyle(style);
                reevaluate.run();
            });
            styles.add(item);
            menu.add(item);
        }
        menu.addSeparator();
        JCheckBoxMenuItem wilderness = styleMenuItem(new JCheckBoxMenuItem("Avoid Wilderness",
            plannerPreferences.isAvoidWilderness()));
        wilderness.addActionListener(event ->
        {
            plannerPreferences.setAvoidWilderness(wilderness.isSelected());
            reevaluate.run();
        });
        menu.add(wilderness);
        menu.addSeparator();
        ButtonGroup sessions = new ButtonGroup();
        for (SessionLength session : SessionLength.values())
        {
            JRadioButtonMenuItem item = styleMenuItem(new JRadioButtonMenuItem("Session: " + session.getLabel(),
                plannerPreferences.getSessionLength() == session));
            item.addActionListener(event ->
            {
                plannerPreferences.setSessionLength(session);
                reevaluate.run();
            });
            sessions.add(item);
            menu.add(item);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void showView(String view)
    {
        cards.show(cardHost, view);
        updateNavigationState(overviewNav, HOME.equals(view));
        updateNavigationState(pathNav, BROWSER.equals(view));
        updateNavigationState(gearNav, GEAR.equals(view));
    }

    private static void updateNavigationState(JButton button, boolean selected)
    {
        button.putClientProperty(PremiumButtonUI.SELECTED_PROPERTY, selected);
        button.repaint();
        button.getAccessibleContext().setAccessibleDescription(selected
            ? "Current Iron Compass view" : button.getToolTipText());
    }

    private static JButton navigationButton(String text, String description)
    {
        JButton button = button(text, ButtonStyle.NAVIGATION);
        button.setFocusable(true);
        button.setFont(UiTokens.LABEL.deriveFont(8f));
        button.setMargin(new java.awt.Insets(3, 0, 3, 0));
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleName(text + " view");
        button.getAccessibleContext().setAccessibleDescription(description);
        return button;
    }

    private static JPanel notice(String text)
    {
        JPanel notice = card(labelHtml(escape(text), UiTokens.UNKNOWN));
        notice.setBackground(new Color(45, 40, 28));
        return notice;
    }

    private static JPanel scrollableVerticalPanel()
    {
        return new ScrollableVerticalPanel();
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
