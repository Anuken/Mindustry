package mindustry.entities;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.Units.*;
import mindustry.gen.*;

public class UnitSorts{
    public static final IntIntMap clusterCount = new IntIntMap();
    public static float timer;
    public static Sortf

    closest = Unit::dst2,
    farthest = (u, x, y) -> -u.dst2(x, y),
    strongest = (u, x, y) -> -u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    weakest = (u, x, y) -> u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    mostArmor = (u, x, y) -> -u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastArmor = (u, x, y) -> u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    mostShield = (u, x, y) -> -u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastShield = (u, x, y) -> u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f;

    /**
     * @param distanceWeight higher values make distance less important. Set to <= 0 to ignore distance.
     */
    public static Sortf grouped(float radius){
        return grouped(radius, -1f);
    }
    public static Sortf grouped(float radius, float distanceWeight){
        return (u, x, y) -> {
            if((timer += Time.delta) > 10f || clusterCount.size < 0){
                timer = 0f;
                clusterCount.clear();
                Groups.unit.each(uc -> clusterCount.increment(clusterKey(uc.team, uc.x, uc.y, radius)));
            }
            return -clusterCount.get(clusterKey(u.team, u.x, u.y, radius), 0) + (distanceWeight > 0 ? Mathf.dst2(u.x, u.y, x, y) / distanceWeight : 0f);
        };
    }

    //8 bits for team. Does not support negative coords
    private static int clusterKey(Team team, float x, float y, float radius){
        return (team.id << 24) | ((Mathf.floor(x / radius) & 0xFFF) << 12) | (Mathf.floor(y / radius) & 0xFFF);
    }

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);
}