package com.ironpath.persistence;

import com.ironpath.route.Route;
import com.ironpath.gear.GearPreferenceStore;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

public final class IronPathPersistence implements ManualOverrideStore, GearPreferenceStore
{
    static final String CONFIG_GROUP = "ironpath-progress";
    static final String OVERRIDES_KEY = "manualOverrides";
    static final String ROUTE_VERSION_KEY = "routeVersion";
    static final String SELECTED_GEAR_GOAL_KEY = "selectedGearGoal";
    static final String SKIPPED_GEAR_KEY = "skippedGear";
    static final String OPTIONAL_GEAR_KEY = "optionalGear";
    static final String ALTERNATIVE_GEAR_KEY = "gearAlternatives";
    static final String GEAR_STYLE_FILTER_KEY = "gearStyleFilter";
    static final String GEAR_STATUS_FILTER_KEY = "gearStatusFilter";

    private final ConfigManager configManager;
    private Map<String, ManualOverride> overrides;
    private String selectedGearGoal;
    private Set<String> skippedGear;
    private Set<String> optionalGear;
    private Map<String, String> gearAlternatives;
    private String gearStyleFilter;
    private String gearStatusFilter;

    @Inject
    public IronPathPersistence(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    @Override
    public ManualOverride get(String stepId)
    {
        load();
        return overrides.get(stepId);
    }

    @Override
    public void put(String stepId, ManualOverride override)
    {
        load();
        if (override == null)
        {
            overrides.remove(stepId);
        }
        else
        {
            overrides.put(stepId, override);
        }
        save();
    }

    @Override
    public void remove(String stepId)
    {
        load();
        if (overrides.remove(stepId) != null)
        {
            save();
        }
    }

    @Override
    public void clear()
    {
        load();
        overrides.clear();
        save();
    }

    @Override
    public Map<String, ManualOverride> snapshot()
    {
        load();
        return Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
    }

    public void migrate(Route route)
    {
        load();
        boolean changed = false;
        for (Route.RouteMigration migration : route.getMigrations())
        {
            ManualOverride old = overrides.remove(migration.getFromStepId());
            if (old != null)
            {
                overrides.putIfAbsent(migration.getToStepId(), old);
                changed = true;
            }
        }
        Integer storedVersion = configManager.getRSProfileConfiguration(CONFIG_GROUP, ROUTE_VERSION_KEY, Integer.class);
        if (storedVersion == null || storedVersion != route.getVersion() || changed)
        {
            configManager.setRSProfileConfiguration(CONFIG_GROUP, ROUTE_VERSION_KEY, route.getVersion());
            save();
        }
    }

    public void profileChanged()
    {
        overrides = null;
        selectedGearGoal = null;
        skippedGear = null;
        optionalGear = null;
        gearAlternatives = null;
        gearStyleFilter = null;
        gearStatusFilter = null;
    }

    @Override
    public String getSelectedGoalId()
    {
        loadGearPreferences();
        return selectedGearGoal == null || selectedGearGoal.isEmpty() ? null : selectedGearGoal;
    }

    @Override
    public void setSelectedGoalId(String goalId)
    {
        loadGearPreferences();
        selectedGearGoal = goalId == null ? "" : goalId;
        saveGearPreferences();
    }

    @Override
    public boolean isSkipped(String goalId)
    {
        loadGearPreferences();
        return skippedGear.contains(goalId);
    }

    @Override
    public void setSkipped(String goalId, boolean skipped)
    {
        loadGearPreferences();
        if (skipped) skippedGear.add(goalId); else skippedGear.remove(goalId);
        saveGearPreferences();
    }

    @Override
    public boolean isMarkedOptional(String goalId)
    {
        loadGearPreferences();
        return optionalGear.contains(goalId);
    }

    @Override
    public void setMarkedOptional(String goalId, boolean optional)
    {
        loadGearPreferences();
        if (optional) optionalGear.add(goalId); else optionalGear.remove(goalId);
        saveGearPreferences();
    }

    @Override
    public String getChosenAlternative(String goalId)
    {
        loadGearPreferences();
        return gearAlternatives.get(goalId);
    }

    @Override
    public void chooseAlternative(String goalId, String alternativeId)
    {
        loadGearPreferences();
        if (alternativeId == null || alternativeId.isEmpty()) gearAlternatives.remove(goalId);
        else gearAlternatives.put(goalId, alternativeId);
        saveGearPreferences();
    }

    @Override
    public String getGearStyleFilter()
    {
        loadGearPreferences();
        return gearStyleFilter;
    }

    @Override
    public void setGearStyleFilter(String style)
    {
        loadGearPreferences();
        gearStyleFilter = defaultFilter(style);
        saveGearPreferences();
    }

    @Override
    public String getGearStatusFilter()
    {
        loadGearPreferences();
        return gearStatusFilter;
    }

    @Override
    public void setGearStatusFilter(String status)
    {
        loadGearPreferences();
        gearStatusFilter = defaultFilter(status);
        saveGearPreferences();
    }

    @Override
    public void resetGearPreferences()
    {
        loadGearPreferences();
        selectedGearGoal = "";
        skippedGear.clear();
        optionalGear.clear();
        gearAlternatives.clear();
        gearStyleFilter = "ALL";
        gearStatusFilter = "ALL";
        saveGearPreferences();
    }

    private void load()
    {
        if (overrides != null)
        {
            return;
        }
        overrides = new LinkedHashMap<>();
        String encoded = configManager.getRSProfileConfiguration(CONFIG_GROUP, OVERRIDES_KEY);
        if (encoded == null || encoded.trim().isEmpty())
        {
            return;
        }
        for (String entry : encoded.split(";"))
        {
            int separator = entry.lastIndexOf(':');
            if (separator <= 0 || separator == entry.length() - 1)
            {
                continue;
            }
            try
            {
                overrides.put(entry.substring(0, separator), ManualOverride.valueOf(entry.substring(separator + 1)));
            }
            catch (IllegalArgumentException ignored)
            {
                // Ignore one corrupt entry without losing the rest of the profile.
            }
        }
    }

    private void save()
    {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, ManualOverride> entry : overrides.entrySet())
        {
            if (encoded.length() > 0)
            {
                encoded.append(';');
            }
            encoded.append(entry.getKey()).append(':').append(entry.getValue().name());
        }
        configManager.setRSProfileConfiguration(CONFIG_GROUP, OVERRIDES_KEY, encoded.toString());
    }

    private void loadGearPreferences()
    {
        if (skippedGear != null)
        {
            return;
        }
        selectedGearGoal = value(SELECTED_GEAR_GOAL_KEY, "");
        skippedGear = decodeSet(value(SKIPPED_GEAR_KEY, ""));
        optionalGear = decodeSet(value(OPTIONAL_GEAR_KEY, ""));
        gearAlternatives = decodeMap(value(ALTERNATIVE_GEAR_KEY, ""));
        gearStyleFilter = defaultFilter(value(GEAR_STYLE_FILTER_KEY, "ALL"));
        gearStatusFilter = defaultFilter(value(GEAR_STATUS_FILTER_KEY, "ALL"));
    }

    private void saveGearPreferences()
    {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, SELECTED_GEAR_GOAL_KEY, selectedGearGoal);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, SKIPPED_GEAR_KEY, String.join(";", skippedGear));
        configManager.setRSProfileConfiguration(CONFIG_GROUP, OPTIONAL_GEAR_KEY, String.join(";", optionalGear));
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : gearAlternatives.entrySet())
        {
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(entry.getKey()).append(':').append(entry.getValue());
        }
        configManager.setRSProfileConfiguration(CONFIG_GROUP, ALTERNATIVE_GEAR_KEY, encoded.toString());
        configManager.setRSProfileConfiguration(CONFIG_GROUP, GEAR_STYLE_FILTER_KEY, gearStyleFilter);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, GEAR_STATUS_FILTER_KEY, gearStatusFilter);
    }

    private String value(String key, String fallback)
    {
        String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
        return stored == null ? fallback : stored;
    }

    private static Set<String> decodeSet(String encoded)
    {
        Set<String> values = new HashSet<>();
        if (encoded == null || encoded.trim().isEmpty()) return values;
        for (String value : encoded.split(";"))
        {
            if (!value.trim().isEmpty()) values.add(value);
        }
        return values;
    }

    private static Map<String, String> decodeMap(String encoded)
    {
        Map<String, String> values = new LinkedHashMap<>();
        if (encoded == null || encoded.trim().isEmpty()) return values;
        for (String value : encoded.split(";"))
        {
            int separator = value.indexOf(':');
            if (separator > 0 && separator < value.length() - 1)
            {
                values.put(value.substring(0, separator), value.substring(separator + 1));
            }
        }
        return values;
    }

    private static String defaultFilter(String value)
    {
        return value == null || value.trim().isEmpty() ? "ALL" : value;
    }
}
