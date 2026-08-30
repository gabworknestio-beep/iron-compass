package com.ironcompass.training;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IronmanMethodCatalog
{
    private int version;
    private String auditedAt;
    private List<IronmanMethodDefinition> methods;
    private List<String> fullGuideSkills;
    private List<TrainingMilestone> milestones;

    public int getVersion() { return version; }
    public String getAuditedAt() { return auditedAt; }
    public List<IronmanMethodDefinition> getMethods()
    {
        return methods == null ? Collections.emptyList() : methods;
    }

    public List<String> getFullGuideSkills()
    {
        return fullGuideSkills == null ? Collections.emptyList() : fullGuideSkills;
    }

    public List<TrainingMilestone> getMilestones()
    {
        return milestones == null ? Collections.emptyList() : milestones;
    }

    public boolean hasFullGuide(String skill)
    {
        for (String value : getFullGuideSkills()) if (value.equalsIgnoreCase(skill)) return true;
        return false;
    }

    public List<TrainingMilestone> milestonesFor(String skill, int afterLevel, int targetLevel)
    {
        List<TrainingMilestone> result = new ArrayList<>();
        for (TrainingMilestone milestone : getMilestones())
            if (milestone.getSkill().equalsIgnoreCase(skill) && milestone.getLevel() > afterLevel
                && milestone.getLevel() <= targetLevel)
                result.add(milestone);
        result.sort(java.util.Comparator.comparingInt(TrainingMilestone::getLevel)
            .thenComparing(TrainingMilestone::getId));
        return Collections.unmodifiableList(result);
    }

    public List<IronmanMethodDefinition> search(String query)
    {
        String needle = normalize(query);
        if (needle.isEmpty()) return getMethods();
        List<IronmanMethodDefinition> result = new ArrayList<>();
        for (IronmanMethodDefinition method : getMethods())
        {
            StringBuilder text = new StringBuilder().append(method.getSkill()).append(' ')
                .append(method.getTitle()).append(' ').append(method.getDescription()).append(' ')
                .append(String.join(" ", method.getTags())).append(' ')
                .append(String.join(" ", method.getStyles())).append(' ')
                .append(String.join(" ", method.getAcquisitionSources())).append(' ')
                .append(String.join(" ", method.getUsefulOutputs()));
            if (normalize(text.toString()).contains(needle)) result.add(method);
        }
        return Collections.unmodifiableList(result);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('_', ' ')
            .replace('-', ' ');
    }
}
