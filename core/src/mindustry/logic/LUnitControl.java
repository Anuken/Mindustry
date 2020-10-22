package mindustry.logic;

public enum LUnitControl{
    stop,
    move("x", "y"),
    approach("x", "y", "radius"),
    boost("enable"),
    pathfind(),
    target("x", "y", "shoot"),
    targetp("unit", "shoot"),
    itemDrop("to", "amount"),
    itemTake("from", "item", "amount"),
    payDrop,
    payTake("takeUnits"),
    mine("x", "y"),
    flag("value"),
    build("x", "y", "block", "rotation"),
    getBlock("x", "y", "type", "building", "constructing"),
    within("x", "y", "radius", "result");

    public final String[] params;
    public static final LUnitControl[] all = values();

    LUnitControl(String... params){
        this.params = params;
    }
}
