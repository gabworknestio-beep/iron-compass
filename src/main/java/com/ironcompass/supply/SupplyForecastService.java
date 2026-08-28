package com.ironcompass.supply;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.SupplySpec;
import com.ironcompass.gear.SupplyVariant;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.List;

public final class SupplyForecastService
{
    public SupplyForecast evaluate(GearEvaluation goal, AccountState state)
    {
        if (goal == null || goal.getStatus() == GearStatus.SKIPPED)
        {
            return null;
        }
        List<SupplyLine> lines = new ArrayList<>();
        for (SupplySpec supply : goal.getUpgrade().getSupplies())
        {
            int carried = 0;
            int banked = 0;
            for (SupplyVariant variant : supply.getVariants())
            {
                carried += state.exactCarriedQuantity(variant.getItemId()) * variant.getUnits();
                if (state.getBank().isObserved())
                {
                    banked += state.getBank().exactQuantity(variant.getItemId()) * variant.getUnits();
                }
            }
            int actual = carried + banked;
            TruthValue status = actual >= supply.getRequiredUnits() ? TruthValue.TRUE
                : state.getBank().isObserved() ? TruthValue.FALSE : TruthValue.UNKNOWN;
            lines.add(new SupplyLine(supply.getName(), actual, supply.getRequiredUnits(), supply.getUnitLabel(),
                supply.isEstimated(), status));
        }
        return new SupplyForecast(goal, lines);
    }
}
