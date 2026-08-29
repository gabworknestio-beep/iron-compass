package com.ironcompass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** User-facing release version generated from the single Gradle property. */
public final class IronCompassVersion
{
    private static final String VERSION = load();

    private IronCompassVersion() { }

    public static String get()
    {
        return VERSION;
    }

    private static String load()
    {
        try (InputStream input = IronCompassVersion.class.getResourceAsStream("/iron-compass-version.properties"))
        {
            if (input == null) return "development";
            Properties values = new Properties();
            values.load(input);
            return values.getProperty("version","development");
        }
        catch (IOException ignored)
        {
            return "development";
        }
    }
}
