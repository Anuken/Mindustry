package mindustry.logic;

import mindustry.logic.LogicNode.*;
import mindustry.logic.SavedLogic.*;

public class LogicExecutor{
    /** all logical operations to be executed */
    private LogicOp[] ops = {};
    /** current operation index */
    private int index;

    /** Set up executor state based on saved data. */
    public void setup(SavedLogic save){
        LogicNode[] out = new LogicNode[save.nodes.length];

        //copy over the state so connections can be assigned
        for(int i = 0; i < out.length; i++){
            out[i] = save.nodes[i].state;
        }

        for(int i = 0; i < out.length; i++){
            LogicNode node = out[i];
            NodeSlot[] slots = node.slots();

            //connect node's slots to other nodes' slots
            for(int j = 0; j < save.nodes[i].connections.length; j++){
                SavedConnection con = save.nodes[i].connections[j];
                NodeSlot slot = slots[j];

                if(con.node != -1){
                    LogicNode other = out[con.node];
                    NodeSlot[] otherSlots = other.slots();
                    NodeSlot otherSlot = otherSlots[con.slot];

                    //slot.objOutput = (aslot, aobj) -> ((ObjOutput)otherSlot.objOutput).set(other, aobj);
                }
            }
        }
    }

    /** A single logical statement. */
    static class LogicOp{

    }
}
