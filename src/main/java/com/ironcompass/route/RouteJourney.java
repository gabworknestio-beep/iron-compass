package com.ironcompass.route;

import java.util.Collections;
import java.util.List;

public final class RouteJourney
{
    private final List<RouteChapterProgress> chapters;
    private final RouteChapterProgress current;

    public RouteJourney(List<RouteChapterProgress> chapters, RouteChapterProgress current)
    {
        this.chapters = Collections.unmodifiableList(chapters);
        this.current = current;
    }

    public List<RouteChapterProgress> getChapters() { return chapters; }
    public RouteChapterProgress getCurrent() { return current; }
}
