package com.ironcompass.route;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class RouteLoader
{
    private final Gson gson;

    public RouteLoader(Gson gson)
    {
        this.gson = gson;
    }

    public Route loadResource(String resource) throws RouteLoadException
    {
        InputStream stream = RouteLoader.class.getResourceAsStream(resource);
        if (stream == null)
        {
            throw new RouteLoadException("Route resource not found: " + resource);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            return load(reader, resource);
        }
        catch (IOException ex)
        {
            throw new RouteLoadException("Unable to close route resource: " + resource, ex);
        }
    }

    public Route load(Reader reader, String sourceName) throws RouteLoadException
    {
        try
        {
            Route route = gson.fromJson(reader, Route.class);
            if (route == null)
            {
                throw new RouteLoadException("Route is empty: " + sourceName);
            }
            return route;
        }
        catch (JsonParseException ex)
        {
            throw new RouteLoadException("Malformed route JSON in " + sourceName + ": " + ex.getMessage(), ex);
        }
    }
}
