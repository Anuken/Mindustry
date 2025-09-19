package mindustry.entities;

import arc.math.*;
import arc.struct.IntSeq;
import mindustry.content.*;
import mindustry.entities.Units.*;
import mindustry.gen.*;

public class UnitSorts{
    private static final IntSeq count = new IntSeq(1);
    public static Sortf

    closest = Unit::dst2,
    farthest = (u, x, y) -> -u.dst2(x, y),
    strongest = (u, x, y) -> -u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    weakest = (u, x, y) -> u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    //Fires at roughly the center of a unit cluster.
    grouped = (u, x, y) -> {
        if (count.size < 1) count.add(0);
        else count.set(0, 0);
        Groups.unit.intersect(u.x - 140, u.y - 140, 140 * 2f, 140 * 2f, other -> {
            if (other.team != u.team && u.dst2(other) <= 140) {
                count.set(0, count.get(0) + 1);
            }
        });
        return -count.get(0) + Mathf.dst2(u.x, u.y, x, y) / 10000f;
    };

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);
}