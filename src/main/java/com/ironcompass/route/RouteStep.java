package com.ironcompass.route;

import com.ironcompass.requirement.ConditionSpec;
import java.util.Collections;
import java.util.List;

public final class RouteStep
{
    private String id;
    private StepType type;
    private String title;
    private String category;
    private String instruction;
    private String reason;
    private ConditionSpec completion;
    private ConditionSpec readiness;
    private List<String> requires;
    private List<PreparationSpec> preparation;
    private List<WhileHereSpec> whileHere;
    private LocationSpec location;
    private String wikiPage;
    private String questHelperKey;
    private List<String> tags;
    private boolean optional;
    private RiskLevel risk = RiskLevel.SAFE;
    private String importance;
    private String hcimAlternativeStepId;
    private String alternativeForStepId;

    public String getId() { return id; }
    public StepType getType() { return type; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getInstruction() { return instruction; }
    public String getReason() { return reason; }
    public ConditionSpec getCompletion() { return completion; }
    public ConditionSpec getReadiness() { return readiness; }
    public List<String> getRequires() { return requires == null ? Collections.emptyList() : requires; }
    public List<PreparationSpec> getPreparation() { return preparation == null ? Collections.emptyList() : preparation; }
    public List<WhileHereSpec> getWhileHere() { return whileHere == null ? Collections.emptyList() : whileHere; }
    public LocationSpec getLocation() { return location; }
    public String getWikiPage() { return wikiPage; }
    public String getQuestHelperKey() { return questHelperKey; }
    public List<String> getTags() { return tags == null ? Collections.emptyList() : tags; }
    public boolean isOptional() { return optional; }
    public RiskLevel getRisk() { return risk; }
    public String getImportance() { return importance == null ? "NORMAL" : importance; }
    public String getHcimAlternativeStepId() { return hcimAlternativeStepId; }
    public String getAlternativeForStepId() { return alternativeForStepId; }
}
