package mindustry.logic;

public enum LNode{
    power,
    bridge,

    add(true),
    remove(false);

    public final boolean mode;

    public static final LNode[]
        get = {power, bridge},
        set = {add, remove};

    LNode(){
        mode = false;
    }

    LNode(boolean mode){
        this.mode = mode;
    }
}
