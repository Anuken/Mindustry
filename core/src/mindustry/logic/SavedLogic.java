package mindustry.logic;

/** The saved state of a logic board. */
public class SavedLogic{
    public SavedNode[] nodes;

    public SavedLogic(SavedNode[] nodes){
        this.nodes = nodes;
    }

    public static class SavedNode{
        /** Uninitialized state containing only relevant configuration. */
        public LogicNode state;
        /** Connections of this node. */
        public SavedConnection[] connections;
        /** x/y positions of the bottom left corner of the node */
        public float x, y;
    }

    public static class SavedConnection{
        /** Node ID (in the array) that is being connected to. -1 means no connection */
        public int node;
        /** Slot number in the node */
        public int slot;
    }
}
