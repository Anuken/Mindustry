package mindustry.game;

import arc.util.*;
import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.io.legacy.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.*;

/**
 * A spawn group defines spawn information for a specific type of unit, with optional extra information like
 * weapon equipped, ammo used, and status effects.
 * Each spawn group can have multiple sub-groups spawned in different areas of the map.
 */
public class SpawnGroup implements Serializable{
    public static final int never = Integer.MAX_VALUE;

    /** The unit type spawned */
    public UnitType type = UnitTypes.dagger;
    /** Payload Seq, by default one dagger */
    public @Nullable Seq<UnitType> payloads;
    /** When this spawn should end */
    public int end = never;
    /** When this spawn should start */
    public int begin;
    /** The spacing, in waves, of spawns. For example, 2 = spawns every other wave */
    public int spacing = 1;
    /** Maximum amount of units that spawn */
    public int max = 40;
    /** How many waves need to pass before the amount of units spawned increases by 1 */
    public float unitScaling = never;
    /** Shield points that this unit has. */
    public float shields = 0f;
    /** How much shields get increased per wave. */
    public float shieldScaling = 0f;
    /** Amount of enemies spawned initially, with no scaling */
    public int unitAmount = 1;
    /** Status effect applied to the spawned unit. Null to disable. */
    @Nullable
    public StatusEffect effect;
    /** Items this unit spawns with. Null to disable. */
    @Nullable
    public ItemStack items;

    public SpawnGroup(UnitType type){
        this.type = type;
    }

    public SpawnGroup(){
        //serialization use only
    }

    /** @return amount of units spawned on a specific wave. */
    public int getSpawned(int wave){
        if(spacing == 0) spacing = 1;
        if(wave < begin || wave > end || (wave - begin) % spacing != 0){
            return 0;
        }
        return Math.min(unitAmount + (int)(((wave - begin) / spacing) / unitScaling), max);
    }

    /** @return amount of shields each unit has at a specific wave. */
    public float getShield(int wave){
        return Math.max(shields + shieldScaling*(wave - begin), 0);
    }

    /**
     * Creates a unit, and assigns correct values based on this group's data.
     * This method does not add() the unit.
     */
    public Unit createUnit(Team team, int wave){
        Unit unit = type.create(team);

        if(effect != null){
            unit.apply(effect, 999999f);
        }

        if(items != null){
            unit.addItem(items.item, items.amount);
        }

        if(payloads != null && payloads.size > 0 && unit instanceof Payloadc){
            for(UnitType unitType : payloads){
                Unit payload = unitType.create(unit.team);
                ((Payloadc) unit).pickup(payload);
            }
        }

        unit.shield = getShield(wave);

        return unit;
    }

    @Override
    public void write(Json json){
        if(type == null) type = UnitTypes.dagger;
        json.writeValue("type", type.name);
        if(begin != 0) json.writeValue("begin", begin);
        if(end != never) json.writeValue("end", end);
        if(spacing != 1) json.writeValue("spacing", spacing);
        if(max != 40) json.writeValue("max", max);
        if(unitScaling != never) json.writeValue("scaling", unitScaling);
        if(shields != 0) json.writeValue("shields", shields);
        if(shieldScaling != 0) json.writeValue("shieldScaling", shieldScaling);
        if(unitAmount != 1) json.writeValue("amount", unitAmount);
        if(effect != null) json.writeValue("effect", effect.name);
        if(payloads != null && payloads.size > 0){
            Seq<String> payloadStrings = new Seq<String>();
            for(UnitType unitType : payloads){
                payloadStrings.add(unitType.name);
            }
            json.writeValue("payloads", payloadStrings);
        }
    }

    @Override
    public void read(Json json, JsonValue data){
        String tname = data.getString("type", "dagger");

        type = content.getByName(ContentType.unit, LegacyIO.unitMap.get(tname, tname));
        if(type == null) type = UnitTypes.dagger;
        begin = data.getInt("begin", 0);
        end = data.getInt("end", never);
        spacing = data.getInt("spacing", 1);
        max = data.getInt("max", 40);
        unitScaling = data.getFloat("scaling", never);
        shields = data.getFloat("shields", 0);
        shieldScaling = data.getFloat("shieldScaling", 0);
        unitAmount = data.getInt("amount", 1);
        if(data.has("payloads")){
            payloads = new Seq<UnitType>();
            Seq<String> payloadStrings = json.readValue(Seq.class, String.class, data.get("payloads"));

            for(String name : payloadStrings){
                payloads.add(content.getByName(ContentType.unit, name));
            }
        }


        //old boss effect ID
        if(data.has("effect") && data.get("effect").isNumber() && data.getInt("effect", -1) == 8){
            effect = StatusEffects.boss;
        }else{
            effect = content.getByName(ContentType.status, data.has("effect") && data.get("effect").isString() ? data.getString("effect", "none") : "none");
        }
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

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        SpawnGroup group = (SpawnGroup)o;
        return end == group.end && begin == group.begin && spacing == group.spacing && max == group.max
            && Float.compare(group.unitScaling, unitScaling) == 0 && Float.compare(group.shields, shields) == 0
            && Float.compare(group.shieldScaling, shieldScaling) == 0 && unitAmount == group.unitAmount &&
            type == group.type && effect == group.effect && Structs.eq(items, group.items);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(new Object[]{type, end, begin, spacing, max, unitScaling, shields, shieldScaling, unitAmount, effect, items});
    }
}
