package mindustry.entities;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.Units.*;
import mindustry.gen.*;

public class UnitSorts{
    private static final Seq<UnitCluster> clusters = new Seq<>();
    public static Sortf

    closest = Unit::dst2,
    farthest = (u, x, y) -> -u.dst2(x, y),
    strongest = (u, x, y) -> -u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    weakest = (u, x, y) -> u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    mostArmor = (u, x, y) -> -u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastArmor = (u, x, y) -> u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    mostShield = (u, x, y) -> -u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastShield = (u, x, y) -> u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f;

    public static UnitCluster grouped(float radius){
        return grouped(radius, -1f);
    }

    /** @param distanceWeight higher values make distance less important. Set to <= 0 to ignore distance. */
    public static UnitCluster grouped(float radius, float distanceWeight){
        for(UnitCluster c : clusters){
            if(c.radius == radius && c.dstWeight == distanceWeight) return c;
        }
        //only allocate if no same cluster (radius, distance) is found
        UnitCluster c = new UnitCluster(radius, distanceWeight);
        clusters.add(c);
        return c;
    }

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);
}