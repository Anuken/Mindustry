package mindustry.logic;

import arc.struct.*;

/** Setter/getter enum for logic-controlled objects. */
public enum LAccess{
    totalItems,
    firstItem,
    totalLiquids,
    totalPower,
    totalPowerNodes,
    totalBridgeNodes,
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
    currentPowerNode,
    currentBridgeLink,
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
    maxUnits,
    id,

    //values with parameters are considered controllable
    enabled("to"), //"to" is standard for single parameter access
    shoot("x", "y", "shoot"),
    shootp(true, "unit", "shoot"),
    config(true, "to"),
    color("to"),
    powerConfig(true,"to", "mode"),
    bridgeConfig(true, "to", "mode"),
    ;

    public final String[] params;
    public final boolean isObj;

    public static final LAccess[]
        all = values(),
        senseable = Seq.select(all, t -> t.params.length <= 1).toArray(LAccess.class),
        senseable2 = {currentPowerNode, currentBridgeLink}, //I named it like this is because this sensor has 2 arguments instead of 1.,
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
