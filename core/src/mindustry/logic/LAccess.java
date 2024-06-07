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
    currentAmmoType(true),
    health,
    maxHealth,
    heat,
    shield,
    armor,
    efficiency,
    progress,
    timescale,
    rotation,
    x,
    y,
    velocityX(true),
    velocityY(true),
    shootX,
    shootY,
    cameraX,
    cameraY,
    cameraWidth,
    cameraHeight,
    size,
    dead,
    range, 
    shooting,
    boosting,
    mineX,
    mineY,
    mining,
    speed,
    team,
    type,
    flag,
    controlled,
    controller,
    name,
    payloadCount,
    payloadType,
    id,

    //values with parameters are considered controllable
    enabled("to"), //"to" is standard for single parameter access
    shoot("x", "y", "shoot"),
    shootp(true, "unit", "shoot"),
    config(true, "to"),
    color("to");

    public final String[] params;
    public final boolean isObj, privileged;

    public static final LAccess[]
        all = values(),
        senseable = Seq.select(all, t -> t.params.length <= 1 && !t.privileged).toArray(LAccess.class),
        privilegeSenseable = Seq.select(all, t -> t.params.length <= 1 && t.privileged).toArray(LAccess.class),
        controls = Seq.select(all, t -> t.params.length > 0).toArray(LAccess.class),
        settable = {x, y, rotation, speed, armor, health, shield, team, flag, totalPower, payloadType};

    LAccess(String... params){
        this.params = params;
        isObj = false;
        this.privileged = false;
    }

    LAccess(boolean obj, String... params){
        this.params = params;
        isObj = obj;
        this.privileged = false;
    }

    LAccess(boolean privileged){
        this.params = new String[0];
        isObj = false;
        this.privileged = privileged;
    }
}
