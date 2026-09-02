package com.ironcompass;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class PublicationReadinessTest
{
    @Test
    public void pluginHubMetadataIsStandardAndMatchesDescriptor() throws Exception
    {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get("runelite-plugin.properties")))
        {
            metadata.load(input);
        }

        assertEquals("Iron Compass", metadata.getProperty("displayName"));
        assertEquals("com.ironcompass.IronCompassPlugin", metadata.getProperty("plugins"));
        assertEquals("standard", metadata.getProperty("build"));
        assertEquals("Gaby100amis", metadata.getProperty("author"));
        assertFalse("Public author metadata is required", blank(metadata.getProperty("author")));
        assertFalse("Replace generic contributor metadata before publication",
            metadata.getProperty("author").toLowerCase().contains("contributors"));
        assertTrue("Description should be useful in Plugin Hub search",
            metadata.getProperty("description", "").length() >= 40);
        assertFalse("Plugin Hub versions are commit-pinned; keep the optional version field absent",
            metadata.containsKey("version"));

        PluginDescriptor descriptor = IronCompassPlugin.class.getAnnotation(PluginDescriptor.class);
        assertNotNull(descriptor);
        assertEquals(metadata.getProperty("displayName"), descriptor.name());
        assertEquals(metadata.getProperty("description"), descriptor.description());
        assertArrayEquals(metadata.getProperty("tags").split(","), descriptor.tags());

        ConfigGroup configGroup = IronCompassConfig.class.getAnnotation(ConfigGroup.class);
        assertNotNull(configGroup);
        assertEquals("ironcompass", configGroup.value());

        Set<String> configKeys = Arrays.stream(IronCompassConfig.class.getDeclaredMethods())
            .map(method -> method.getAnnotation(ConfigItem.class))
            .filter(annotation -> annotation != null)
            .map(ConfigItem::keyName)
            .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(
            "preferSafeAlternatives", "wikiActions", "shortestPath",
            "completionNotifications", "preparationLookahead")), configKeys);

        Class<?> pluginClass = Class.forName(metadata.getProperty("plugins"));
        assertEquals(IronCompassPlugin.class, pluginClass);
        assertTrue(Plugin.class.isAssignableFrom(pluginClass));
    }

    @Test
    public void publicIconMeetsPluginHubLimitsAndMatchesBundledResource() throws Exception
    {
        Path publicIcon = Paths.get("icon.png");
        Path bundledIcon = Paths.get("src", "main", "resources", "icon.png");
        BufferedImage image = ImageIO.read(publicIcon.toFile());

        assertNotNull("Root icon must be a valid image", image);
        assertTrue("Plugin Hub icon width must be at most 48 px", image.getWidth() <= 48);
        assertTrue("Plugin Hub icon height must be at most 72 px", image.getHeight() <= 72);
        assertTrue("Root and bundled icons must stay identical",
            Arrays.equals(Files.readAllBytes(publicIcon), Files.readAllBytes(bundledIcon)));
    }

    @Test
    public void publicReleaseDocumentationExists()
    {
        for (String file : Arrays.asList("README.md", "LICENSE", "CHANGELOG.md", "CONTRIBUTING.md",
            "SECURITY.md", "THIRD_PARTY_NOTICES.md", "docs/PLUGIN_HUB_CHECKLIST.md",
            "docs/PLUGIN_HUB_SUBMISSION.md", "docs/GOAL_PLANNER.md"))
        {
            assertTrue("Missing public release document: " + file, Files.isRegularFile(Paths.get(file)));
        }
        assertFalse("Do not register a Plugin SPI service from an external plugin",
            Files.exists(Paths.get("src", "main", "resources", "META-INF", "services",
                "net.runelite.client.plugins.Plugin")));
    }

    @Test
    public void publicVersionComesFromOneReleaseProperty() throws Exception
    {
        Properties gradle = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get("gradle.properties")))
        {
            gradle.load(input);
        }
        assertEquals("1.1.5",gradle.getProperty("pluginVersion"));
        assertEquals("1.1.5",IronCompassVersion.get());
        assertFalse(new String(Files.readAllBytes(Paths.get("runelite-plugin.properties")),
            java.nio.charset.StandardCharsets.UTF_8).contains("1.5.0"));
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
