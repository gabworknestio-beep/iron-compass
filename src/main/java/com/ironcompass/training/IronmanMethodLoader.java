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
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;

public final class IronmanMethodLoader
{
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
            if (!Set.of("SAFE", "WILDERNESS").contains(method.getRisk().toUpperCase(Locale.ENGLISH)))
                throw new MethodLoadException("Invalid method risk in " + source + ": " + method.getId());
            validateCondition(method.getRequirements(), source, method.getId());
            for (MethodResourceGroup group : method.getResourceInputs())
                if (blank(group.getLabel()) || group.getItemIds().isEmpty()
                    || group.getItemIds().stream().anyMatch(id -> id == null || id <= 0))
                    throw new MethodLoadException("Invalid resource group in " + source + ": " + method.getId());
        }
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
