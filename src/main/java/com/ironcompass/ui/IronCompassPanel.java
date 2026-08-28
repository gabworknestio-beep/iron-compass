package com.ironcompass.ui;

import com.ironcompass.IronCompassConfig;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearPreferenceStore;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalAction;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalResolution;
import com.ironcompass.integration.QuestHelperBridge;
import com.ironcompass.integration.ShortestPathBridge;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.planner.GoalPlanProjection;
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
import com.ironcompass.training.SkillTrainingAdvisor;
import com.ironcompass.training.TrainingAdvice;
import com.ironcompass.training.IronmanMethodDefinition;
import com.ironcompass.training.MethodRecommendation;
import com.ironcompass.training.MethodResourceStatus;
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
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public final class IronCompassPanel extends PluginPanel
{
    private static final SkillTrainingAdvisor TRAINING_ADVISOR = new SkillTrainingAdvisor();
    private static final RouteJourneyService JOURNEY_SERVICE = new RouteJourneyService();
    private static final GoalPickerModel GOAL_PICKER = new GoalPickerModel();
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
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JButton overviewNav = navigationButton("OVERVIEW", "Open the account overview");
    private final JButton pathNav = navigationButton("PATH", "Browse the progression route");
    private final JButton gearNav = navigationButton("GEAR", "Browse gear objectives");
    private final JPanel home = scrollableVerticalPanel();
    private final JPanel browserResults = scrollableVerticalPanel();
    private final GearPathPanel gearPanel;
    private final JTextField search = new JTextField();
    private final JButton goalPickerButton = smallButton("CHOOSE GOALS");
    private final JButton pathBack = smallButton("BACK");
    private RouteProjection projection;
    private GearProjection gearProjection;
    private GoalResolution goalResolution;
    private SupplyForecast supplyForecast;
    private GoalPlanProjection goalPlan;
    private RecommendationProjection recommendations;
    private RouteJourney journey;
    private AccountState accountState = AccountState.loggedOut();
    private String routeDetailId;
    private boolean showUsefulBreaks;

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
            JButton alternatives = smallButton(showUsefulBreaks ? "HIDE OTHER PROGRESS" : "TAKE A USEFUL BREAK");
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
        body.add(gap(4));
        if (goalPlan.hasSelectedGoal())
            body.add(labelHtml("<span style='color:#8f8f8f'>PRIMARY</span><br><b>" + escape(goalPlan.getTitle())
                + "</b>", UiTokens.ACCENT));
        else
            body.add(labelHtml("No primary goal selected.", UiTokens.MUTED));
        if (!goalPlan.getSecondaryGoals().isEmpty())
        {
            body.add(gap(5));
            body.add(sectionLabel("SECONDARY"));
            for (GoalPlanProjection secondary : goalPlan.getSecondaryGoals())
                body.add(labelHtml("○  " + escape(secondary.getTitle()), UiTokens.TEXT));
        }
        body.add(gap(6));
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
                body.add(gap(8));
                body.add(sectionLabel("NEXT BEST MOVE"));
                body.add(gap(3));
                body.add(labelHtml("<b>" + escape(action.getTitle()) + "</b>",
                    action.getKind() == PlannedAction.Kind.COMPLETE ? UiTokens.SUCCESS : UiTokens.TEXT));
                body.add(gap(7));
                body.add(sectionLabel("WHY THIS?"));
                body.add(gap(3));
                ProgressionCandidate ranked = recommendations == null ? null : recommendations.getRecommended();
                if (recommendationMatchesPrimaryAction(ranked))
                    for (String why : ranked.getWhyLines()) body.add(labelHtml("•  " + escape(why), UiTokens.MUTED));
                else
                    body.add(labelHtml(escape(goalPlan.getWhyNow()), UiTokens.MUTED));
                addMethodRecommendation(body);
            }
            addGoalProgress(body);
            if (goalPlan.getAfterThis() != null)
            {
                body.add(gap(7));
                body.add(sectionLabel("AFTER THIS"));
                body.add(gap(3));
                body.add(labelHtml(escape(goalPlan.getAfterThis()), UiTokens.TEXT));
            }
            body.add(gap(7));
            body.add(sectionLabel("WHAT THIS UNLOCKS"));
            body.add(gap(3));
            int shown = 0;
            for (String unlock : goalPlan.getUnlocks())
            {
                body.add(labelHtml("○  " + escape(unlock), UiTokens.TEXT));
                if (++shown == 1) break;
            }
            if (goalPlan.getUnlocks().size() > shown)
                body.add(labelHtml("+ " + (goalPlan.getUnlocks().size() - shown) + " more unlock(s)", UiTokens.MUTED));
            ResourceReadiness resources = goalPlan.getResourceReadiness();
            if (resources != null)
            {
                body.add(gap(7));
                body.add(sectionLabel("RESOURCE READINESS"));
                body.add(gap(3));
                body.add(statusLine(resources.getValue(), resources.getSummary()));
            }
        }
        body.add(gap(8));
        body.add(goalPlannerActions());
        body.add(gap(3));
        body.add(labelHtml(humanize(plannerPreferences.getPlaystyle().name()) + " · "
            + plannerPreferences.getSessionLength().getLabel()
            + (plannerPreferences.isAvoidWilderness() ? " · Avoid Wilderness" : ""), UiTokens.MUTED));
        return card(body);
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
        if (goalPlan.hasSelectedGoal() || goalPlan.getUnavailableSelectedId() != null)
        {
            JButton clear = smallButton("CLEAR PRIMARY");
            clear.setToolTipText("Clear the primary progression goal");
            clear.addActionListener(event ->
            {
                gearPreferences.setPrimaryGoalId(null);
                reevaluate.run();
            });
            row.add(clear);
        }
        if (goalPlan.hasSelectedGoal() && goalPlan.getWikiPage() != null)
        {
            JButton wiki = smallButton("WIKI");
            wiki.addActionListener(event -> wikiBridge.open(goalPlan.getWikiPage()));
            row.add(wiki);
        }
        JButton preferences = smallButton("PREFERENCES");
        preferences.setToolTipText("Change playstyle, Wilderness, and session ranking preferences");
        preferences.addActionListener(event -> showPlannerPreferences(preferences));
        row.add(preferences);
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
        for (String why : candidate.getWhyLines()) body.add(labelHtml("•  " + escape(why), UiTokens.TEXT));
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
        return card(body);
    }

    private void addMethodRecommendation(JPanel body)
    {
        MethodRecommendation recommendation = goalPlan.getMethodRecommendation();
        if (recommendation == null || recommendation.getRecommended() == null) return;
        IronmanMethodDefinition method = recommendation.getRecommended();
        body.add(gap(8));
        body.add(sectionLabel("GOOD FIT FOR THIS ACCOUNT"));
        body.add(gap(3));
        body.add(labelHtml("<b>" + escape(method.getTitle()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(escape(method.getDescription()), UiTokens.TEXT));
        body.add(gap(3));
        body.add(labelHtml(escape(recommendation.getReason()), UiTokens.MUTED));
        MethodResourceStatus resource = recommendation.getResourceStatus();
        if (resource != MethodResourceStatus.NOT_APPLICABLE)
        {
            TruthValue value = resource == MethodResourceStatus.SUFFICIENT ? TruthValue.TRUE
                : resource == MethodResourceStatus.EMPTY ? TruthValue.FALSE : TruthValue.UNKNOWN;
            body.add(gap(5));
            body.add(sectionLabel("RESOURCE READINESS"));
            body.add(statusLine(value, recommendation.getResourceSummary()));
            if ((resource == MethodResourceStatus.EMPTY || resource == MethodResourceStatus.PARTIAL)
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
        if (!recommendation.getAlternatives().isEmpty())
            body.add(labelHtml("Alternative: " + escape(recommendation.getAlternatives().get(0).getTitle()),
                UiTokens.MUTED));
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
        return card(body);
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
        return card(body);
    }

    private JPanel buildHeader()
    {
        JPanel panel = verticalPanel();
        panel.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("IRON COMPASS");
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
            showView(GEAR);
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
            showView(GEAR);
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
            showView(GEAR);
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
        JScrollPane scroll = new JScrollPane(browserResults);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTokens.BACKGROUND);
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
        home.add(sectionLabel("IRON COMPASS"));
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
    }

    private void showGoalPicker()
    {
        if (goalPlan == null || goalPlan.getCatalog() == null) return;
        JTextField goalSearch = new JTextField();
        JComboBox<String> category = new JComboBox<>(GOAL_PICKER.categories(goalPlan.getCatalog()).toArray(new String[0]));
        JComboBox<String> role = new JComboBox<>(new String[]{"Set as primary", "Add as secondary", "Remove from active"});
        DefaultListModel<GoalChoice> choices = new DefaultListModel<>();
        JList<GoalChoice> list = new JList<>(choices);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        Runnable refresh = () ->
        {
            choices.clear();
            Set<String> active = new HashSet<>();
            for (GoalPlanProjection plan : goalPlan.getActiveGoals()) active.add(plan.getGoalId());
            for (GoalDefinition goal : GOAL_PICKER.filter(goalPlan.getCatalog(), goalSearch.getText(),
                (String) category.getSelectedItem(), active, accountState, gearProjection))
            {
                String marker = goal.getId().equals(gearPreferences.getPrimaryGoalId()) ? "PRIMARY  ·  "
                    : gearPreferences.getSecondaryGoalIds().contains(goal.getId()) ? "SECONDARY  ·  " : "";
                choices.addElement(new GoalChoice(goal.getId(), marker + goal.getTitle()));
            }
            if (!choices.isEmpty()) list.setSelectedIndex(0);
        };
        goalSearch.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { refresh.run(); }
            @Override public void removeUpdate(DocumentEvent event) { refresh.run(); }
            @Override public void changedUpdate(DocumentEvent event) { refresh.run(); }
        });
        category.addActionListener(event -> refresh.run());
        refresh.run();
        JPanel picker = new JPanel(new BorderLayout(5, 5));
        JPanel filters = new JPanel(new GridLayout(0, 1, 3, 3));
        filters.add(new JLabel("Search"));
        filters.add(goalSearch);
        filters.add(category);
        filters.add(role);
        picker.add(filters, BorderLayout.NORTH);
        picker.add(new JScrollPane(list), BorderLayout.CENTER);
        picker.setPreferredSize(new Dimension(330, 360));
        int answer = JOptionPane.showConfirmDialog(this, picker, "Iron Compass Goal Picker",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        GoalChoice selected = list.getSelectedValue();
        if (answer != JOptionPane.OK_OPTION || selected == null) return;
        String selectedRole = (String) role.getSelectedItem();
        if ("Set as primary".equals(selectedRole)) gearPreferences.setPrimaryGoalId(selected.id);
        else if ("Add as secondary".equals(selectedRole))
        {
            if (!gearPreferences.addSecondaryGoalId(selected.id))
                JOptionPane.showMessageDialog(this, "Secondary goals are limited to three and cannot duplicate the primary goal.",
                    "Goal queue", JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            if (selected.id.equals(gearPreferences.getPrimaryGoalId())) gearPreferences.setPrimaryGoalId(null);
            gearPreferences.removeSecondaryGoalId(selected.id);
        }
        reevaluate.run();
    }

    private void showPlannerPreferences(Component anchor)
    {
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup styles = new ButtonGroup();
        for (Playstyle style : Playstyle.values())
        {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(humanize(style.name()),
                plannerPreferences.getPlaystyle() == style);
            item.addActionListener(event ->
            {
                plannerPreferences.setPlaystyle(style);
                reevaluate.run();
            });
            styles.add(item);
            menu.add(item);
        }
        menu.addSeparator();
        JCheckBoxMenuItem wilderness = new JCheckBoxMenuItem("Avoid Wilderness",
            plannerPreferences.isAvoidWilderness());
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
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("Session: " + session.getLabel(),
                plannerPreferences.getSessionLength() == session);
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
        button.setEnabled(!selected);
        button.getAccessibleContext().setAccessibleDescription(selected
            ? "Current Iron Compass view" : button.getToolTipText());
    }

    private static JButton navigationButton(String text, String description)
    {
        JButton button = smallButton(text);
        button.setFocusable(true);
        button.setFont(UiTokens.LABEL.deriveFont(8f));
        button.setMargin(new java.awt.Insets(3, 0, 3, 0));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTokens.BORDER),
            BorderFactory.createEmptyBorder(3, 0, 3, 0)));
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
        JLabel label = new JLabel("<html><table width='155' cellspacing='0' cellpadding='0'><tr><td>"
            + body + "</td></tr></table></html>");
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
        button.setFocusable(true);
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

    private static final class GoalChoice
    {
        private final String id;
        private final String label;

        private GoalChoice(String id, String label)
        {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }
}
