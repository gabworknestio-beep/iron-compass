package com.ironcompass.route;

public final class RouteLoadException extends Exception
{
    public RouteLoadException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RouteLoadException(String message)
    {
        super(message);
    }
}
