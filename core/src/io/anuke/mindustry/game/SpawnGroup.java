package io.anuke.mindustry.game;

import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.GroundUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.type.Weapon;

/**
 * A spawn group defines spawn information for a specific type of unit, with optional extra information like
 * weapon equipped, ammo used, and status effects.
 * Each spawn group can have multiple sub-groups spawned in different areas of the map.
 */
public class SpawnGroup{
    /**
     * The unit type spawned
     */
    public final UnitType type;
    /**
     * When this spawn should end
     */
    protected int end = Integer.MAX_VALUE;
    /**
     * When this spawn should start
     */
    protected int begin;
    /**
     * The spacing, in waves, of spawns. For example, 2 = spawns every other wave
     */
    protected int spacing = 1;
    /**
     * Maximum amount of units that spawn
     */
    protected int max = 60;
    /**
     * How many waves need to pass before the amount of units spawned increases by 1
     */
    protected float unitScaling = 9999f;
    /**
     * How many waves need to pass before the amount of instances of this group increases by 1
     */
    protected float groupScaling = 9999f;
    /**
     * Amount of enemies spawned initially, with no scaling
     */
    protected int unitAmount = 1;
    /**
     * Amount of enemies spawned initially, with no scaling
     */
    protected int groupAmount = 1;
    /**
     * Weapon used by the spawned unit. Null to disable. Only applicable to ground units.
     */
    protected Weapon weapon;
    /**
     * Status effect applied to the spawned unit. Null to disable.
     */
    protected StatusEffect effect;
    /**
     * Items this unit spawns with. Null to disable.
     */
    protected ItemStack items;

    public SpawnGroup(UnitType type){
        this.type = type;
    }

    /**
     * Returns the amount of units spawned on a specific wave.
     */
    public int getUnitsSpawned(int wave){
        if(wave < begin || wave > end || (wave - begin) % spacing != 0){
            return 0;
        }
        float scaling = this.unitScaling;

        return Math.min(unitAmount - 1 + Math.max((int) ((wave / spacing) / scaling), 1), max);
    }

    /**
     * Returns the amount of different unit groups at a specific wave.
     */
    public int getGroupsSpawned(int wave){
        if(wave < begin || wave > end || (wave - begin) % spacing != 0){
            return 0;
        }

        return Math.min(groupAmount - 1 + Math.max((int) ((wave / spacing) / groupScaling), 1), max);
    }

    /**
     * Creates a unit, and assigns correct values based on this group's data.
     * This method does not add() the unit.
     */
    public BaseUnit createUnit(Team team){
        BaseUnit unit = type.create(team);

        if(unit instanceof GroundUnit && weapon != null){
            ((GroundUnit) unit).setWeapon(weapon);
        }

        if(effect != null){
            unit.applyEffect(effect, 10000f);
        }

        if(items != null){
            unit.inventory.addItem(items.item, items.amount);
        }

        return unit;
    }
}
