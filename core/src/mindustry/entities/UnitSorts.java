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
    weakest = (u, x, y) -> u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f;

    public static Sortf grouped(float radius){
        return (u, x, y) -> {
            updateClusters(radius);
            int key = (Mathf.floor(u.x / radius) << 16) | Mathf.floor(u.y / radius);
            //ignore distance since in less dense groups it almost always wins
            return -clusterCount.get(key, 0);
        };
    }

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);

    public static void updateClusters(float radius){
        if((timer += Time.delta) < 10f && clusterCount.size > 0) return;
        timer = 0f;
        clusterCount.clear();

        Groups.unit.each(u -> {
            if(!u.isEnemy()) return;
            clusterCount.increment((Mathf.floor(u.x / radius) << 16) | Mathf.floor(u.y / radius));
        });
    }
}