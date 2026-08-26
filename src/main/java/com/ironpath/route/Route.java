package com.ironpath.route;

import java.util.Collections;
import java.util.List;

public final class Route
{
    private String routeId;
    private int version;
    private String name;
    private String description;
    private String auditedAt;
    private List<RouteSource> sources;
    private List<RouteChapterSpec> chapters;
    private List<RouteSection> sections;
    private List<RouteMigration> migrations;

    public String getRouteId()
    {
        return routeId;
    }

    public int getVersion()
    {
        return version;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getAuditedAt()
    {
        return auditedAt;
    }

    public List<RouteSource> getSources()
    {
        return sources == null ? Collections.emptyList() : sources;
    }

    public List<RouteSection> getSections()
    {
        return sections == null ? Collections.emptyList() : sections;
    }

    public List<RouteChapterSpec> getChapters()
    {
        return chapters == null ? Collections.emptyList() : chapters;
    }

    public List<RouteMigration> getMigrations()
    {
        return migrations == null ? Collections.emptyList() : migrations;
    }

    public static final class RouteSource
    {
        private String type;
        private String title;
        private String url;
        private String auditedAt;
        private String notes;

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getAuditedAt() { return auditedAt; }
        public String getNotes() { return notes; }
    }

    public static final class RouteMigration
    {
        private String fromStepId;
        private String toStepId;

        public String getFromStepId() { return fromStepId; }
        public String getToStepId() { return toStepId; }
    }
}
