package com.ironpath.goal;

import java.util.List;

public final class GoalValidationException extends Exception
{
    public GoalValidationException(List<String> errors)
    {
        super(String.join("; ", errors));
    }
}
