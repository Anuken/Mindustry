package mindustry.logic;

import arc.struct.*;

/** Setter/getter enum for logic-controlled objects. */
public enum LAccess{
    totalItems,
    firstItem,
    totalLiquids,
    totalPower,
    itemCapacity,
    liquidCapacity,
    powerCapacity,
    powerNetStored,
    powerNetCapacity,
    powerNetIn,
    powerNetOut,
    ammo,
    ammoCapacity,
    health,
    maxHealth,
    heat,
    efficiency,
    rotation,
    x,
    y,
    shootX,
    shootY,
    shooting,
    mineX,
    mineY,
    mining,
    team,
    type,
    flag,
    controlled,
    commanded,
    name,
    config,
    payloadCount,
    payloadType,

    //values with parameters are considered controllable
    enabled("to"), //"to" is standard for single parameter access
    shoot("x", "y", "shoot"),
    shootp(true, "unit", "shoot"),
    configure(true, 30, "to");

    public final String[] params;
    public final boolean isObj;
    /** Tick cooldown between invocations. */
    public float cooldown = -1;

    public static final LAccess[]
        all = values(),
        senseable = Seq.select(all, t -> t.params.length <= 1).toArray(LAccess.class),
        controls = Seq.select(all, t -> t.params.length > 0).toArray(LAccess.class);

    LAccess(String... params){
        this.params = params;
        isObj = false;
    }

    LAccess(boolean obj, String... params){
        this.params = params;
        isObj = obj;
    }

    LAccess(boolean obj, float cooldown, String... params){
        this.params = params;
        this.cooldown = cooldown;
        isObj = obj;
    }
}
