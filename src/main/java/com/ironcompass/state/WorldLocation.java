package com.ironcompass.state;

import java.util.Objects;

public final class WorldLocation
{
    private final int x;
    private final int y;
    private final int plane;

    public WorldLocation(int x, int y, int plane)
    {
        this.x = x;
        this.y = y;
        this.plane = plane;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getPlane()
    {
        return plane;
    }

    public int distanceTo(WorldLocation other)
    {
        if (other == null || plane != other.plane)
        {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof WorldLocation))
        {
            return false;
        }
        WorldLocation that = (WorldLocation) other;
        return x == that.x && y == that.y && plane == that.plane;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(x, y, plane);
    }
}
