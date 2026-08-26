package com.ironpath.route;

public final class PreparationEvaluation
{
    private final PreparationSpec preparation;
    private final PreparationStatus status;
    private final int actual;
    private final int required;

    public PreparationEvaluation(PreparationSpec preparation, PreparationStatus status, int actual, int required)
    {
        this.preparation = preparation;
        this.status = status;
        this.actual = actual;
        this.required = required;
    }

    public PreparationSpec getPreparation() { return preparation; }
    public PreparationStatus getStatus() { return status; }
    public int getActual() { return actual; }
    public int getRequired() { return required; }
}
