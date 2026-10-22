package mindustry.logic;

public enum LogicUnitControl{
    idle,
    stop,
    move("x", "y"),
    approach("x", "y", "radius"),
    pathfind("x", "y"),
    autoPathfind,
    boost("enable"),
    target("x", "y", "shoot"),
    targetp("unit", "shoot"),
    itemDrop("to", "amount"),
    itemTake("from", "item", "amount"),
    payDrop,
    payTake("takeUnits"),
    payEnter,
    mine("x", "y"),
    flag("value"),
    build("x", "y", "block", "rotation", "config"),
    deconstruct("x", "y"),
    getBlock("x", "y", "type", "building", "floor"),
    within("x", "y", "radius", "result"),
    unbind;

    public final String[] params;
    public static final LogicUnitControl[] all = values();

    LogicUnitControl(String... params){
        this.params = params;
    }
}
