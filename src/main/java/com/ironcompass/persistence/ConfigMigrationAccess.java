package com.ironcompass.persistence;

interface ConfigMigrationAccess
{
    String get(String group, String key);

    void set(String group, String key, Object value);

    String getProfile(String group, String key);

    void setProfile(String group, String key, Object value);
}
