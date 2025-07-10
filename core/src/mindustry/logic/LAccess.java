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
    currentAmmoType,
    memoryCapacity,
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
    velocityX,
    velocityY,
    shootX,
    shootY,
    cameraX,
    cameraY,
    cameraWidth,
    cameraHeight,
    displayWidth,
    displayHeight,
    bufferSize,
    operations,
    size,
    solid,
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
    totalPayload,
    payloadCapacity,
    id,

    //values with parameters are considered controllable
    enabled("to"), //"to" is standard for single parameter access
    shoot("x", "y", "shoot"),
    shootp(true, "unit", "shoot"),
    config(true, "to"),
    color("to");

    public final String[] params;
    public final boolean isObj;

    public static final LAccess[]
        all = values(),
        senseable = Seq.select(all, t -> t.params.length <= 1).toArray(LAccess.class),
        controls = Seq.select(all, t -> t.params.length > 0).toArray(LAccess.class),
        settable = {x, y, velocityX, velocityY, rotation, speed, armor, health, shield, team, flag, totalPower, payloadType};

    LAccess(String... params){
        this.params = params;
        isObj = false;
    }

    LAccess(boolean obj, String... params){
        this.params = params;
        isObj = obj;
    }
}
