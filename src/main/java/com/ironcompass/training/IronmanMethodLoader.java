package com.ironcompass.training;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;

public final class IronmanMethodLoader
{
    private static final Set<String> VALID_SKILLS = Set.of(
        "attack", "strength", "defence", "ranged", "prayer", "magic", "runecraft", "construction",
        "hitpoints", "agility", "herblore", "thieving", "crafting", "fletching", "slayer", "hunter",
        "mining", "smithing", "fishing", "cooking", "firemaking", "woodcutting", "farming", "sailing");
    private static final Set<String> VALID_STYLES = Set.of(
        "EFFICIENT", "ACTIVE", "CHILL", "AFK", "RESOURCE_POSITIVE", "LOW_COST");
    private final Gson gson;

    public IronmanMethodLoader(Gson gson) { this.gson = gson; }

    public IronmanMethodCatalog loadResource(String resource) throws MethodLoadException
    {
        try (InputStream stream = IronmanMethodLoader.class.getResourceAsStream(resource))
        {
            if (stream == null) throw new MethodLoadException("Method catalog not found: " + resource);
            return load(new InputStreamReader(stream, StandardCharsets.UTF_8), resource);
        }
        catch (IOException ex)
        {
            throw new MethodLoadException("Unable to close method catalog: " + resource, ex);
        }
    }

    public IronmanMethodCatalog load(Reader reader, String source) throws MethodLoadException
    {
        try
        {
            IronmanMethodCatalog catalog = gson.fromJson(reader, IronmanMethodCatalog.class);
            validate(catalog, source);
            return catalog;
        }
        catch (RuntimeException ex)
        {
            throw new MethodLoadException("Unable to parse method catalog " + source, ex);
        }
    }

    private static void validate(IronmanMethodCatalog catalog, String source) throws MethodLoadException
    {
        if (catalog == null || catalog.getVersion() < 1 || catalog.getMethods().isEmpty())
            throw new MethodLoadException("Method catalog is empty or unversioned: " + source);
        Set<String> ids = new HashSet<>();
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            if (blank(method.getId()) || !ids.add(method.getId()) || blank(method.getSkill())
                || blank(method.getTitle()) || blank(method.getDescription()) || blank(method.getWikiPage())
                || method.getMinLevel() < 1 || method.getMaxLevel() > 99
                || method.getMinLevel() > method.getMaxLevel())
                throw new MethodLoadException("Invalid method definition in " + source + ": " + method.getId());
            if (!VALID_SKILLS.contains(method.getSkill().toLowerCase(Locale.ENGLISH)))
                throw new MethodLoadException("Invalid method skill in " + source + ": " + method.getSkill());
            if (!Set.of("SAFE", "WILDERNESS").contains(method.getRisk().toUpperCase(Locale.ENGLISH)))
                throw new MethodLoadException("Invalid method risk in " + source + ": " + method.getId());
            if (method.getRecommendationPriority() < 0 || method.getRecommendationPriority() > 100
                || method.getIronmanValue() < 1 || method.getIronmanValue() > 5
                || method.getXpRateMin() < 0 || method.getXpRateMax() < method.getXpRateMin())
                throw new MethodLoadException("Invalid method scoring or XP range in " + source + ": "
                    + method.getId());
            for (String style : method.getStyles())
                if (!VALID_STYLES.contains(style.toUpperCase(Locale.ENGLISH)))
                    throw new MethodLoadException("Invalid method style in " + source + ": " + method.getId());
            validateReferences(method.getSourceReferences(), source, method.getId());
            validateCondition(method.getRequirements(), source, method.getId());
            for (MethodResourceGroup group : method.getResourceInputs())
                if (blank(group.getLabel()) || group.getItemIds().isEmpty()
                    || group.getItemIds().stream().anyMatch(id -> id == null || id <= 0))
                    throw new MethodLoadException("Invalid resource group in " + source + ": " + method.getId());
        }
        validateMilestones(catalog, source, ids);
        validateCoverage(catalog, source);
    }

    private static void validateMilestones(IronmanMethodCatalog catalog, String source, Set<String> ids)
        throws MethodLoadException
    {
        for (TrainingMilestone milestone : catalog.getMilestones())
        {
            if (blank(milestone.getId()) || !ids.add(milestone.getId()) || blank(milestone.getSkill())
                || blank(milestone.getTitle()) || blank(milestone.getIronmanValue())
                || milestone.getLevel() < 1 || milestone.getLevel() > 99
                || !VALID_SKILLS.contains(milestone.getSkill().toLowerCase(Locale.ENGLISH)))
                throw new MethodLoadException("Invalid training milestone in " + source + ": "
                    + milestone.getId());
            validateReferences(milestone.getSourceReferences(), source, milestone.getId());
        }
    }

    private static void validateCoverage(IronmanMethodCatalog catalog, String source) throws MethodLoadException
    {
        Map<String, boolean[]> coverage = new HashMap<>();
        for (String skill : catalog.getFullGuideSkills())
        {
            if (!VALID_SKILLS.contains(skill.toLowerCase(Locale.ENGLISH)))
                throw new MethodLoadException("Invalid full-guide skill in " + source + ": " + skill);
            coverage.put(skill.toLowerCase(Locale.ENGLISH), new boolean[100]);
        }
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            boolean[] levels = coverage.get(method.getSkill().toLowerCase(Locale.ENGLISH));
            if (levels == null || !method.isTrainingMethod()) continue;
            for (int level = method.getMinLevel(); level <= method.getMaxLevel(); level++) levels[level] = true;
        }
        for (Map.Entry<String, boolean[]> entry : coverage.entrySet())
            for (int level = 1; level <= 99; level++)
                if (!entry.getValue()[level])
                    throw new MethodLoadException("Full guide has a level gap in " + source + ": "
                        + entry.getKey() + " " + level);
    }

    private static void validateReferences(List<String> references, String source, String id)
        throws MethodLoadException
    {
        if (references.isEmpty()) throw new MethodLoadException("Missing method references in " + source + ": " + id);
        for (String reference : references)
            if (blank(reference) || !reference.startsWith("https://"))
                throw new MethodLoadException("Invalid method reference in " + source + ": " + id);
    }

    private static void validateCondition(ConditionSpec condition, String source, String methodId)
        throws MethodLoadException
    {
        if (condition == null) return;
        if (!ConditionEvaluator.supports(condition.getType()))
            throw new MethodLoadException("Unsupported method condition in " + source + ": " + methodId);
        if ("SKILL_AT_LEAST".equalsIgnoreCase(condition.getType())
            && (blank(condition.getSkill()) || condition.getLevel() < 1 || condition.getLevel() > 99))
            throw new MethodLoadException("Invalid skill gate in " + source + ": " + methodId);
        if ("QUEST_STATE".equalsIgnoreCase(condition.getType()) && blank(condition.getQuest()))
            throw new MethodLoadException("Invalid quest gate in " + source + ": " + methodId);
        for (ConditionSpec child : condition.getChildren()) validateCondition(child, source, methodId);
        validateCondition(condition.getChild(), source, methodId);
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
