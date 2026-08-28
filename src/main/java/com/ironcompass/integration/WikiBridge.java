package com.ironcompass.integration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import net.runelite.client.util.LinkBrowser;

public final class WikiBridge
{
    private static final String BASE_URL = "https://oldschool.runescape.wiki/w/";

    @Inject
    public WikiBridge()
    {
    }

    public void open(String page)
    {
        if (page == null || page.trim().isEmpty())
        {
            return;
        }
        LinkBrowser.browse(urlFor(page));
    }

    static String urlFor(String page)
    {
        String trimmed = page.trim();
        int fragmentIndex = trimmed.indexOf('#');
        String title = fragmentIndex < 0 ? trimmed : trimmed.substring(0, fragmentIndex);
        String fragment = fragmentIndex < 0 ? "" : trimmed.substring(fragmentIndex + 1);
        String url = BASE_URL + encode(title);
        return fragment.isEmpty() ? url : url + "#" + encode(fragment);
    }

    private static String encode(String value)
    {
        return URLEncoder.encode(value.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20");
    }

    public IntegrationStatus status()
    {
        return IntegrationStatus.WORKING;
    }
}
