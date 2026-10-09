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
    mostArmor = (u, x, y) -> -u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastArmor = (u, x, y) -> u.armor + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    mostShield = (u, x, y) -> -u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    leastShield = (u, x, y) -> u.shield + Mathf.dst2(u.x, u.y, x, y) / 6400f,
    grouped = grouped(0.003f);

    public static BuildingPriorityf

    buildingDefault = b -> b.block.priority,
    buildingWater = b -> b.block.priority + (b.liquids != null && b.liquids.get(Liquids.water) > 5f ? 10f : 0f);

    /** @param dstWeight higher values make distance more important. Set to <= 0 to ignore distance. Ignores flying units. */
    public static Sortf grouped(float dstWeight){
        return (u, x, y) -> -u.team.data().getClustered(u.x, u.y) + (dstWeight > 0 ? Mathf.dst(u.x, u.y, x, y) * dstWeight : 0f);
    }
}