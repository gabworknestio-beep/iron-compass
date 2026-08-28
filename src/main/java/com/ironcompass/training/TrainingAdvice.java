package com.ironcompass.training;

public final class TrainingAdvice
{
    private final String primaryMethod;
    private final String alternativeMethod;
    private final String bankContext;

    public TrainingAdvice(String primaryMethod, String alternativeMethod, String bankContext)
    {
        this.primaryMethod = primaryMethod;
        this.alternativeMethod = alternativeMethod;
        this.bankContext = bankContext;
    }

    public String getPrimaryMethod() { return primaryMethod; }
    public String getAlternativeMethod() { return alternativeMethod; }
    public String getBankContext() { return bankContext; }
}
