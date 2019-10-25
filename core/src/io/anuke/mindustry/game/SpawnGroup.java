package io.anuke.mindustry.game;

import io.anuke.arc.util.serialization.Json;
import io.anuke.arc.util.serialization.Json.Serializable;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.content;

/**
 * A spawn group defines spawn information for a specific type of unit, with optional extra information like
 * weapon equipped, ammo used, and status effects.
 * Each spawn group can have multiple sub-groups spawned in different areas of the map.
 */
public class SpawnGroup implements Serializable{
    public static final int never = Integer.MAX_VALUE;

    /** The unit type spawned */
    public UnitType type;
    /** When this spawn should end */
    public int end = never;
    /** When this spawn should start */
    public int begin;
    /** The spacing, in waves, of spawns. For example, 2 = spawns every other wave */
    public int spacing = 1;
    /** Maximum amount of units that spawn */
    public int max = 100;
    /** How many waves need to pass before the amount of units spawned increases by 1 */
    public float unitScaling = never;
    /** Amount of enemies spawned initially, with no scaling */
    public int unitAmount = 1;
    /** Status effect applied to the spawned unit. Null to disable. */
    public StatusEffect effect;
    /** Items this unit spawns with. Null to disable. */
    public ItemStack items;

    public SpawnGroup(UnitType type){
        this.type = type;
    }

    public SpawnGroup(){
        //serialization use only
    }

    /** Returns the amount of units spawned on a specific wave. */
    public int getUnitsSpawned(int wave){
        if(wave < begin || wave > end || (wave - begin) % spacing != 0){
            return 0;
        }
        return Math.min(unitAmount + (int)(((wave - begin) / spacing) / unitScaling), max);
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
    public void write(Json json){
        json.writeValue("type", type.name);
        if(begin != 0) json.writeValue("begin", begin);
        if(end != never) json.writeValue("end", end);
        if(spacing != 1) json.writeValue("spacing", spacing);
        //if(max != 40) json.writeValue("max", max);
        if(unitScaling != never) json.writeValue("scaling", unitScaling);
        if(unitAmount != 1) json.writeValue("amount", unitAmount);
        if(effect != null) json.writeValue("effect", effect.id);
    }

    @Override
    public void read(Json json, JsonValue data){
        type = content.getByName(ContentType.unit, data.getString("type", "dagger"));
        if(type == null) type = UnitTypes.dagger;
        begin = data.getInt("begin", 0);
        end = data.getInt("end", never);
        spacing = data.getInt("spacing", 1);
        //max = data.getInt("max", 40);
        unitScaling = data.getFloat("scaling", never);
        unitAmount = data.getInt("amount", 1);
        effect = content.getByID(ContentType.status, data.getInt("effect", -1));
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
