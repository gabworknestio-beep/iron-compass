package com.ironpath.gear;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class GearLoader
{
    private final Gson gson;

    public GearLoader(Gson gson)
    {
        this.gson = gson;
    }

    public GearCatalog loadResource(String resource) throws GearLoadException
    {
        try (InputStream stream = GearLoader.class.getResourceAsStream(resource))
        {
            if (stream == null)
            {
                throw new GearLoadException("Missing gear catalog resource: " + resource, null);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                return load(reader, resource);
            }
        }
        catch (IOException ex)
        {
            throw new GearLoadException("Unable to read gear catalog " + resource, ex);
        }
    }

    public GearCatalog load(Reader reader, String source) throws GearLoadException
    {
        try
        {
            GearCatalog catalog = gson.fromJson(reader, GearCatalog.class);
            if (catalog == null)
            {
                throw new GearLoadException("Empty gear catalog: " + source, null);
            }
            return catalog;
        }
        catch (RuntimeException ex)
        {
            throw new GearLoadException("Malformed gear catalog " + source + ": " + ex.getMessage(), ex);
        }
    }
}
