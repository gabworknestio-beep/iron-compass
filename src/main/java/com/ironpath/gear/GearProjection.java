package com.ironpath.gear;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GearProjection
{
    private final GearCatalog catalog;
    private final List<GearEvaluation> evaluations;
    private final GearEvaluation recommended;
    private final GearEvaluation selected;
    private final int ownedCount;
    private final Map<String, GearEvaluation> byId;

    public GearProjection(GearCatalog catalog, List<GearEvaluation> evaluations, GearEvaluation recommended,
                          GearEvaluation selected, int ownedCount)
    {
        this.catalog = catalog;
        this.evaluations = Collections.unmodifiableList(evaluations);
        this.recommended = recommended;
        this.selected = selected;
        this.ownedCount = ownedCount;
        Map<String, GearEvaluation> indexed = new LinkedHashMap<>();
        for (GearEvaluation evaluation : evaluations)
        {
            indexed.put(evaluation.getUpgrade().getId(), evaluation);
        }
        byId = Collections.unmodifiableMap(indexed);
    }

    public GearCatalog getCatalog() { return catalog; }
    public List<GearEvaluation> getEvaluations() { return evaluations; }
    public GearEvaluation getRecommended() { return recommended; }
    public GearEvaluation getSelected() { return selected; }
    public int getOwnedCount() { return ownedCount; }
    public int getTotalCount() { return evaluations.size(); }
    public GearEvaluation find(String id) { return byId.get(id); }
}
