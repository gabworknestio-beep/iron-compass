package com.ironcompass.goal;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class GoalLoader
{
    private final Gson gson;

    public GoalLoader(Gson gson)
    {
        this.gson = gson;
    }

    public GoalCatalog loadResource(String resource) throws GoalLoadException
    {
        try (InputStream stream = GoalLoader.class.getResourceAsStream(resource))
        {
            if (stream == null)
            {
                throw new GoalLoadException("Missing goal catalog resource: " + resource, null);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                return load(reader, resource);
            }
        }
        catch (IOException ex)
        {
            throw new GoalLoadException("Unable to read goal catalog " + resource, ex);
        }
    }

    public GoalCatalog load(Reader reader, String source) throws GoalLoadException
    {
        try
        {
            GoalCatalog catalog = gson.fromJson(reader, GoalCatalog.class);
            if (catalog == null)
            {
                throw new GoalLoadException("Empty goal catalog: " + source, null);
            }
            return catalog;
        }
        catch (RuntimeException ex)
        {
            throw new GoalLoadException("Malformed goal catalog " + source + ": " + ex.getMessage(), ex);
        }
    }
}
