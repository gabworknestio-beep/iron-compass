package com.ironpath.requirement;

public enum TruthValue
{
    TRUE,
    FALSE,
    UNKNOWN;

    public TruthValue not()
    {
        if (this == TRUE)
        {
            return FALSE;
        }
        if (this == FALSE)
        {
            return TRUE;
        }
        return UNKNOWN;
    }
}
