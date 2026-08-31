package com.ironcompass;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoadException;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.GearValidationException;
import com.ironcompass.gear.GearValidator;
import com.ironcompass.goal.GoalDependencyResolver;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalLoadException;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.goal.GoalResolution;
import com.ironcompass.goal.GoalValidationException;
import com.ironcompass.goal.GoalValidator;
import com.ironcompass.integration.QuestHelperBridge;
import com.ironcompass.integration.ShortestPathBridge;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.IronCompassConfigMigration;
import com.ironcompass.persistence.IronCompassPersistence;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.GoalPlannerService;
import com.ironcompass.planner.ProgressionRecommendationService;
import com.ironcompass.planner.RecommendationProjection;
import com.ironcompass.planner.UnlockOpportunity;
import com.ironcompass.planner.UnlockRadarService;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoadException;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RouteValidationException;
import com.ironcompass.route.RouteValidator;
import com.ironcompass.route.RouteVariables;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.AccountStateService;
import com.ironcompass.supply.SupplyForecast;
import com.ironcompass.supply.SupplyForecastService;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodLoader;
import com.ironcompass.training.MethodLoadException;
import com.ironcompass.training.MethodPlannerService;
import com.ironcompass.ui.IronCompassPanel;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Iron Compass",
    description = "OSRS Ironman progression planner for RuneLite with account-aware goals, gear upgrades, quest routing, and skill guides.",
    tags = {"ironman", "osrs", "progression", "planner", "goals", "gear", "skills", "quests", "guide", "route"}
)
public final class IronCompassPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(IronCompassPlugin.class);
    private static final String ROUTE_RESOURCE = "/routes/efficient-ironman.json";
    private static final String GEAR_CATALOG_RESOURCE = "/gear/ironman-gear-2026.json";
    private static final String GOAL_CATALOG_RESOURCE = "/goals/ironman-goals-2026.json";
    private static final String METHOD_CATALOG_RESOURCE = "/methods/ironman-methods-2026.json";
    private static final int NO_CAPTURE_TICK = Integer.MIN_VALUE;
    private static final int CAPTURE_INTERVAL_TICKS = 2;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private Gson gson;
    @Inject private IronCompassConfig config;
    @Inject private AccountStateService accountStateService;
    @Inject private IronCompassConfigMigration configMigration;
    @Inject private IronCompassPersistence persistence;
    @Inject private WikiBridge wikiBridge;
    @Inject private ShortestPathBridge shortestPathBridge;
    @Inject private QuestHelperBridge questHelperBridge;
    @Inject private Notifier notifier;

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    private final RouteEvaluator evaluator = new RouteEvaluator(conditionEvaluator);
    private final GearRecommendationService gearEvaluator = new GearRecommendationService(conditionEvaluator);
    private final GoalDependencyResolver goalResolver = new GoalDependencyResolver();
    private final SupplyForecastService supplyEvaluator = new SupplyForecastService();
    private final GoalPlannerService goalPlanner = new GoalPlannerService(conditionEvaluator, goalResolver,
        supplyEvaluator);
    private final ProgressionRecommendationService recommendationService = new ProgressionRecommendationService();
    private final MethodPlannerService methodPlanner = new MethodPlannerService(conditionEvaluator);
    private final UnlockRadarService unlockRadar = new UnlockRadarService();
    private Route route;
    private GearCatalog gearCatalog;
    private GoalCatalog goalCatalog;
    private IronmanMethodCatalog methodCatalog;
    private RouteVariables routeVariables;
    private AccountState accountState = AccountState.loggedOut();
    private RouteProjection projection;
    private GearProjection gearProjection;
    private GoalResolution goalResolution;
    private SupplyForecast supplyForecast;
    private GoalPlanProjection goalPlan;
    private RecommendationProjection recommendations;
    private IronCompassPanel panel;
    private NavigationButton navigationButton;
    private boolean dirty;
    private boolean questsDirty = true;
    private int lastCaptureTick = NO_CAPTURE_TICK;
    private String lastCurrentStepId;

    @Provides
    IronCompassConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IronCompassConfig.class);
    }

    @Override
    protected void startUp()
    {
        lastCaptureTick = NO_CAPTURE_TICK;
        configMigration.migrate();
        persistence.profileChanged();
        panel = new IronCompassPanel(config, wikiBridge, shortestPathBridge, questHelperBridge, persistence,
            this::requestImmediateReevaluation);
        BufferedImage icon = ImageUtil.loadImageResource(IronCompassPlugin.class, "/icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("Iron Compass")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        try
        {
            route = new RouteLoader(gson).loadResource(ROUTE_RESOURCE);
            gearCatalog = new GearLoader(gson).loadResource(GEAR_CATALOG_RESOURCE);
            goalCatalog = new GoalLoader(gson).loadResource(GOAL_CATALOG_RESOURCE);
            methodCatalog = new IronmanMethodLoader(gson).loadResource(METHOD_CATALOG_RESOURCE);
            Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
            new RouteValidator(quests).validate(route);
            new GearValidator().validate(gearCatalog, route);
            new GoalValidator(quests).validate(goalCatalog, gearCatalog, route);
            routeVariables = new RouteVariables(route);
            persistence.migrate(route);
            panel.setSkillPlanner(methodCatalog, methodPlanner);
            log.debug("Loaded Iron Compass route {} v{}, gear catalog v{}, and {} training methods",
                route.getRouteId(), route.getVersion(), gearCatalog.getVersion(), methodCatalog.getMethods().size());
        }
        catch (RouteLoadException | RouteValidationException | GearLoadException | GearValidationException
            | GoalLoadException | GoalValidationException | MethodLoadException ex)
        {
            log.error("Unable to load Iron Compass route", ex);
            panel.showError(ex.getMessage());
            return;
        }

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            requestImmediateReevaluation();
        }
    }

    @Override
    protected void shutDown()
    {
        dirty = false;
        questsDirty = true;
        lastCaptureTick = NO_CAPTURE_TICK;
        lastCurrentStepId = null;
        projection = null;
        gearProjection = null;
        goalResolution = null;
        supplyForecast = null;
        goalPlan = null;
        recommendations = null;
        route = null;
        gearCatalog = null;
        goalCatalog = null;
        methodCatalog = null;
        routeVariables = null;
        accountStateService.clearSession();
        unlockRadar.reset();
        shortestPathBridge.clear();
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
        }
        panel = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            questsDirty = true;
            lastCaptureTick = NO_CAPTURE_TICK;
            requestImmediateReevaluation();
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            accountStateService.clearSession();
            accountState = AccountState.loggedOut();
            projection = null;
            gearProjection = null;
            goalResolution = null;
            supplyForecast = null;
            goalPlan = null;
            recommendations = null;
            lastCurrentStepId = null;
            unlockRadar.reset();
            if (panel != null)
            {
                panel.update(accountState, null, (GearProjection) null, null, null, null, null);
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        dirty = true;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        dirty = true;
        questsDirty = true;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.BANK)
        {
            accountStateService.observeBank(event.getItemContainer());
        }
        if (event.getContainerId() == InventoryID.BANK
            || event.getContainerId() == InventoryID.INV
            || event.getContainerId() == InventoryID.WORN)
        {
            dirty = true;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!dirty || route == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        int tick = client.getTickCount();
        if (!isCaptureDue(tick, lastCaptureTick))
        {
            return;
        }
        lastCaptureTick = tick;
        attemptRefresh();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (IronCompassConfig.GROUP.equals(event.getGroup()) && route != null)
        {
            evaluateSnapshot(false);
        }
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        resetCharacterContext();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        resetCharacterContext();
    }

    private void resetCharacterContext()
    {
        configMigration.migrateCurrentProfile();
        persistence.profileChanged();
        accountStateService.clearSession();
        accountState = AccountState.loggedOut();
        projection = null;
        gearProjection = null;
        goalResolution = null;
        supplyForecast = null;
        goalPlan = null;
        recommendations = null;
        lastCurrentStepId = null;
        questsDirty = true;
        lastCaptureTick = NO_CAPTURE_TICK;
        shortestPathBridge.clear();
        unlockRadar.reset();
        if (panel != null)
        {
            panel.update(accountState, null, (GearProjection) null, null, null, null, null);
        }
        if (route != null)
        {
            persistence.migrate(route);
            requestImmediateReevaluation();
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if (panel != null && projection != null)
        {
            panel.update(accountState, projection, gearProjection, goalResolution, supplyForecast,
                goalPlan, recommendations);
        }
    }

    private void requestImmediateReevaluation()
    {
        if (route == null)
        {
            return;
        }
        dirty = true;
        clientThread.invokeLater(this::attemptRefresh);
    }

    private void attemptRefresh()
    {
        try
        {
            dirty = !refreshFromClient();
        }
        catch (RuntimeException ex)
        {
            dirty = true;
            log.warn("Unable to refresh Iron Compass account state; will retry", ex);
        }
    }

    private boolean refreshFromClient()
    {
        if (route == null || routeVariables == null)
        {
            return false;
        }
        AccountState captured = accountStateService.capture(
            routeVariables.getVarbits(), routeVariables.getVarps(), questsDirty);
        if (!captured.isLoggedIn())
        {
            return false;
        }
        accountState = captured;
        questsDirty = false;
        evaluateSnapshot(true);
        return true;
    }

    static boolean isCaptureDue(int currentTick, int previousTick)
    {
        return previousTick == NO_CAPTURE_TICK
            || (long) currentTick - previousTick >= CAPTURE_INTERVAL_TICKS;
    }

    private void evaluateSnapshot(boolean allowNotification)
    {
        if (route == null)
        {
            return;
        }
        RouteProjection next = evaluator.evaluate(route, accountState, persistence,
            config.preferSafeAlternatives(), 4, config.preparationLookahead());
        GearProjection nextGear = gearEvaluator.evaluate(gearCatalog, accountState, persistence, persistence);
        GoalResolution nextGoal = goalResolver.resolve(nextGear, next);
        GoalPlanProjection nextPlan = goalPlanner.evaluate(goalCatalog, accountState, nextGear, next, persistence,
            persistence);
        nextPlan = nextPlan.withSkillTrainingPlan(nextPlan.getNextAction() == null
            || nextPlan.getNextAction().getSkill() == null ? null
            : methodPlanner.plan(methodCatalog, nextPlan.getNextAction().getSkill(),
                nextPlan.getNextAction().getTargetLevel(), accountState, persistence, nextPlan.getActiveGoals()));
        RecommendationProjection nextRecommendations = recommendationService.evaluate(next, nextGear, nextPlan,
            accountState, persistence);
        UnlockOpportunity opportunity = unlockRadar.evaluate(nextGear, nextPlan);
        nextRecommendations = nextRecommendations.withNewOpportunity(opportunity);
        SupplyForecast nextSupplies = supplyEvaluator.evaluate(nextGear.getSelected() == null
            ? nextGear.getRecommended() : nextGear.getSelected(), accountState);
        String nextId = nextPlan != null && nextPlan.getNextAction() != null
            ? "planner:" + nextPlan.getNextAction().getTitle()
            : nextGoal != null && nextGoal.getNextAction() != null
            ? "goal:" + nextGoal.getNextAction().getTitle()
            : next.getCurrent() == null ? null : next.getCurrent().getStep().getId();
        if (allowNotification && config.completionNotifications() && lastCurrentStepId != null
            && nextId != null && !lastCurrentStepId.equals(nextId))
        {
            String nextTitle = nextPlan != null && nextPlan.getNextAction() != null
                ? nextPlan.getNextAction().getTitle()
                : nextGoal != null && nextGoal.getNextAction() != null
                ? nextGoal.getNextAction().getTitle()
                : next.getCurrent() == null ? "route complete" : next.getCurrent().getStep().getTitle();
            notifier.notify("Iron Compass advanced. Next: " + nextTitle);
        }
        projection = next;
        gearProjection = nextGear;
        goalResolution = nextGoal;
        supplyForecast = nextSupplies;
        goalPlan = nextPlan;
        recommendations = nextRecommendations;
        lastCurrentStepId = nextId;
        if (panel != null)
        {
            panel.update(accountState, projection, gearProjection, goalResolution, supplyForecast,
                goalPlan, recommendations);
        }
    }
}
