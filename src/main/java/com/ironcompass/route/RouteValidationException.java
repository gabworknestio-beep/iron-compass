package com.ironcompass.route;

import java.util.Collections;
import java.util.List;

public final class RouteValidationException extends Exception
{
    private final List<String> errors;

    public RouteValidationException(List<String> errors)
    {
        super(String.join("; ", errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
