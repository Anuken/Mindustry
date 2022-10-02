package mindustry.logic;

public enum LUnitControl{
    idle,
    stop,
    move("x", "y"),
    approach("x", "y", "radius"),
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
    getBlock("x", "y", "type", "building"),
    within("x", "y", "radius", "result"),
    unbind;

    public final String[] params;
    public static final LUnitControl[] all = values();

    LUnitControl(String... params){
        this.params = params;
    }
}
