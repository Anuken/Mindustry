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
    mine("x", "y"),
    flag("value"),
    within("x", "y", "radius", "result");

    public final String[] params;
    public static final LUnitControl[] all = values();

    LUnitControl(String... params){
        this.params = params;
    }
}
