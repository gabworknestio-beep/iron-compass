package com.ironcompass.state;

public enum AccountMode
{
    UNKNOWN,
    REGULAR,
    IRONMAN,
    HARDCORE_IRONMAN,
    ULTIMATE_IRONMAN,
    GROUP_IRONMAN,
    HARDCORE_GROUP_IRONMAN,
    UNRANKED_GROUP_IRONMAN;

    public boolean isIronman()
    {
        return this != UNKNOWN && this != REGULAR;
    }

    public boolean isHardcore()
    {
        return this == HARDCORE_IRONMAN || this == HARDCORE_GROUP_IRONMAN;
    }

    public boolean isUltimate()
    {
        return this == ULTIMATE_IRONMAN;
    }
}
