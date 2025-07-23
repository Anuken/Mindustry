package mindustry.entities;

import arc.math.*;
import mindustry.content.*;
import mindustry.entities.Units.*;
import mindustry.gen.*;

public class UnitSorts{
    public static Sortf

    closest = Unit::dst2,
    farthest = (u, x, y) -> -u.dst2(x, y),
    strongest = (u, x, y) -> -u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    weakest = (u, x, y) -> u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    //Fires at roughly the center of a unit cluster.
    grouped = (u, x, y) -> {
        int[] count = {0};
        float[] avgX = {0f}, avgY = {0f};
        Groups.unit.each(other -> {
            if (other.team != u.team && u.dst2(other) <  120) {
                count[0]++;
                avgX[0] += other.x;
                avgY[0] += other.y;
                }
            });
        //No cluster.
        if (count[0] == 0) return Float.MAX_VALUE;
        avgX[0] /= count[0];
        avgY[0] /= count[0];
        float distToClusterCenter = Mathf.dst2(u.x, u.y, avgX[0], avgY[0]);
        return distToClusterCenter - count[0] * 100f;
    };

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);
}