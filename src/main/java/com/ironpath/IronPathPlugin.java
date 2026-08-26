package com.ironpath;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoadException;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.GearValidationException;
import com.ironpath.gear.GearValidator;
import com.ironpath.goal.GoalDependencyResolver;
import com.ironpath.goal.GoalResolution;
import com.ironpath.integration.QuestHelperBridge;
import com.ironpath.integration.ShortestPathBridge;
import com.ironpath.integration.WikiBridge;
import com.ironpath.persistence.IronPathPersistence;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.route.Route;
import com.ironpath.route.RouteEvaluator;
import com.ironpath.route.RouteLoadException;
import com.ironpath.route.RouteLoader;
import com.ironpath.route.RouteProjection;
import com.ironpath.route.RouteValidationException;
import com.ironpath.route.RouteValidator;
import com.ironpath.route.RouteVariables;
import com.ironpath.state.AccountState;
import com.ironpath.state.AccountStateService;
import com.ironpath.supply.SupplyForecast;
import com.ironpath.supply.SupplyForecastService;
import com.ironpath.ui.IronPathPanel;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "IronPath",
    description = "Account-aware Ironman progression, gear goals, requirements, supplies, and one explainable next step.",
    tags = {"ironman", "progression", "gear", "quest", "guide", "planning"}
)
public final class IronPathPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(IronPathPlugin.class);
    private static final String ROUTE_RESOURCE = "/routes/efficient-ironman.json";
    private static final String GEAR_CATALOG_RESOURCE = "/gear/ironman-gear-2026.json";
    private static final int NO_CAPTURE_TICK = Integer.MIN_VALUE;
    private static final int CAPTURE_INTERVAL_TICKS = 2;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private Gson gson;
    @Inject private IronPathConfig config;
    @Inject private AccountStateService accountStateService;
    @Inject private IronPathPersistence persistence;
    @Inject private WikiBridge wikiBridge;
    @Inject private ShortestPathBridge shortestPathBridge;
    @Inject private QuestHelperBridge questHelperBridge;
    @Inject private Notifier notifier;

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    private final RouteEvaluator evaluator = new RouteEvaluator(conditionEvaluator);
    private final GearRecommendationService gearEvaluator = new GearRecommendationService(conditionEvaluator);
    private final GoalDependencyResolver goalResolver = new GoalDependencyResolver();
    private final SupplyForecastService supplyEvaluator = new SupplyForecastService();
    private Route route;
    private GearCatalog gearCatalog;
    private RouteVariables routeVariables;
    private AccountState accountState = AccountState.loggedOut();
    private RouteProjection projection;
    private GearProjection gearProjection;
    private GoalResolution goalResolution;
    private SupplyForecast supplyForecast;
    private IronPathPanel panel;
    private NavigationButton navigationButton;
    private boolean dirty;
    private boolean questsDirty = true;
    private int lastCaptureTick = NO_CAPTURE_TICK;
    private String lastCurrentStepId;

    @Provides
    IronPathConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IronPathConfig.class);
    }

    @Override
    protected void startUp()
    {
        lastCaptureTick = NO_CAPTURE_TICK;
        panel = new IronPathPanel(config, wikiBridge, shortestPathBridge, questHelperBridge, persistence,
            this::requestImmediateReevaluation);
        BufferedImage icon = ImageUtil.loadImageResource(IronPathPlugin.class, "/icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("IronPath")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        try
        {
            route = new RouteLoader(gson).loadResource(ROUTE_RESOURCE);
            gearCatalog = new GearLoader(gson).loadResource(GEAR_CATALOG_RESOURCE);
            Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
            new RouteValidator(quests).validate(route);
            new GearValidator().validate(gearCatalog);
            routeVariables = new RouteVariables(route);
            persistence.migrate(route);
            log.debug("Loaded IronPath route {} v{} and gear catalog v{}", route.getRouteId(), route.getVersion(),
                gearCatalog.getVersion());
        }
        catch (RouteLoadException | RouteValidationException | GearLoadException | GearValidationException ex)
        {
            log.error("Unable to load IronPath route", ex);
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
        route = null;
        gearCatalog = null;
        routeVariables = null;
        accountStateService.clearSession();
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
            lastCurrentStepId = null;
            if (panel != null)
            {
                panel.update(accountState, null, (GearProjection) null, null, null);
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
        if (IronPathConfig.GROUP.equals(event.getGroup()) && route != null)
        {
            evaluateSnapshot(false);
        }
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        persistence.profileChanged();
        lastCurrentStepId = null;
        questsDirty = true;
        if (route != null)
        {
            persistence.migrate(route);
            dirty = true;
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        if (panel != null && projection != null)
        {
            panel.update(accountState, projection, gearProjection, goalResolution, supplyForecast);
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
            log.warn("Unable to refresh IronPath account state; will retry", ex);
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
        SupplyForecast nextSupplies = supplyEvaluator.evaluate(nextGear.getSelected() == null
            ? nextGear.getRecommended() : nextGear.getSelected(), accountState);
        String nextId = nextGoal != null && nextGoal.getNextAction() != null
            ? "goal:" + nextGoal.getNextAction().getTitle()
            : next.getCurrent() == null ? null : next.getCurrent().getStep().getId();
        if (allowNotification && config.completionNotifications() && lastCurrentStepId != null
            && nextId != null && !lastCurrentStepId.equals(nextId))
        {
            String nextTitle = nextGoal != null && nextGoal.getNextAction() != null
                ? nextGoal.getNextAction().getTitle()
                : next.getCurrent() == null ? "route complete" : next.getCurrent().getStep().getTitle();
            notifier.notify("IronPath advanced. Next: " + nextTitle);
        }
        projection = next;
        gearProjection = nextGear;
        goalResolution = nextGoal;
        supplyForecast = nextSupplies;
        lastCurrentStepId = nextId;
        if (panel != null)
        {
            panel.update(accountState, projection, gearProjection, goalResolution, supplyForecast);
        }
    }
}
