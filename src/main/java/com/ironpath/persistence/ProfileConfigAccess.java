package com.ironpath.persistence;

import java.lang.reflect.Type;

interface ProfileConfigAccess
{
    String get(String group, String key);

    <T> T get(String group, String key, Type type);

    void set(String group, String key, Object value);
}
