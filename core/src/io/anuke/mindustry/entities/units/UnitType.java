package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;

public class UnitType {
    private static byte lastid = 0;
    private static Array<UnitType> types = new Array<>();

    protected final UnitCreator creator;

    public final String name;
    public final byte id;

    public float health = 60;
    public float hitsize = 5f;
    public float hitsizeTile = 4f;
    public float speed = 0.4f;
    public float range = 160;
    public float rotatespeed = 0.1f;
    public float shootTranslation = 4f;
    public float baseRotateSpeed = 0.1f;
    public float mass = 1f;
    public boolean isFlying;
    public float drag = 0.1f;
    public float maxVelocity = 5f;
    public float reload = 40f;
    public float retreatPercent = 0.2f;
    public float armor = 0f;
    public float carryWeight = 1f;
    public int ammoCapacity = 100;
    public int itemCapacity = 30;
    public int mineLevel = 2;
    public ObjectMap<Item, AmmoType> ammo = new ObjectMap<>();

    public UnitType(String name, UnitCreator creator){
        this.id = lastid++;
        this.name = name;
        this.creator = creator;

        types.add(this);
    }

    public BaseUnit create(Team team){
        return creator.create(team);
    }

    protected void setAmmo(AmmoType... types){
        for(AmmoType type : types){
            ammo.put(type.item, type);
        }
    }

    public interface UnitCreator{
        BaseUnit create(Team team);
    }

    public static UnitType getByID(byte id){
        return types.get(id);
    }

    public static Array<UnitType> getAllTypes(){
        return types;
    }
}
