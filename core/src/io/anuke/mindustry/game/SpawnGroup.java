package io.anuke.mindustry.game;

import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.type.UnitType;

/**
 * A spawn group defines spawn information for a specific type of unit, with optional extra information like
 * weapon equipped, ammo used, and status effects.
 * Each spawn group can have multiple sub-groups spawned in different areas of the map.
 */
public class SpawnGroup{
    protected static final int never = Integer.MAX_VALUE;

    /**The unit type spawned*/
    public final UnitType type;
    /**When this spawn should end*/
    protected int end = never;
    /**When this spawn should start*/
    protected int begin;
    /**The spacing, in waves, of spawns. For example, 2 = spawns every other wave*/
    protected int spacing = 1;
    /**Maximum amount of units that spawn*/
    protected int max = 40;
    /**How many waves need to pass before the amount of units spawned increases by 1*/
    protected float unitScaling = 9999f;
    /**Amount of enemies spawned initially, with no scaling*/
    protected int unitAmount = 1;
    /**Status effect applied to the spawned unit. Null to disable.*/
    protected StatusEffect effect;
    /**Items this unit spawns with. Null to disable.*/
    protected ItemStack items;

    public SpawnGroup(UnitType type){
        this.type = type;
    }

    /**Returns the amount of units spawned on a specific wave.*/
    public int getUnitsSpawned(int wave){
        if(wave < begin || wave > end || (wave - begin) % spacing != 0){
            return 0;
        }
        float scaling = this.unitScaling;

        return Math.min(unitAmount - 1 + Math.max((int) (((wave - begin) / spacing) / scaling), 1), max);
    }

    /**
     * Creates a unit, and assigns correct values based on this group's data.
     * This method does not add() the unit.
     */
    public BaseUnit createUnit(Team team){
        BaseUnit unit = type.create(team);

        if(effect != null){
            unit.applyEffect(effect, 999999f);
        }

        if(items != null){
            unit.addItem(items.item, items.amount);
        }

        return unit;
    }

    @Override
    public String toString(){
        return "SpawnGroup{" +
        "type=" + type +
        ", end=" + end +
        ", begin=" + begin +
        ", spacing=" + spacing +
        ", max=" + max +
        ", unitScaling=" + unitScaling +
        ", unitAmount=" + unitAmount +
        ", effect=" + effect +
        ", items=" + items +
        '}';
    }
}
