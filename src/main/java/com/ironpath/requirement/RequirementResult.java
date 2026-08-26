package com.ironpath.requirement;

public final class RequirementResult
{
    private final TruthValue value;
    private final String label;
    private final String detail;

    public RequirementResult(TruthValue value, String label, String detail)
    {
        this.value = value;
        this.label = label;
        this.detail = detail;
    }

    public TruthValue getValue()
    {
        return value;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDetail()
    {
        return detail;
    }
}
