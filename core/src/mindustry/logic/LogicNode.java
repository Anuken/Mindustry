package mindustry.logic;

import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** Base class for a type of logic node. */
public abstract class LogicNode{

    /** Runs the calculation(s) and sets the output value. */
    public void run(){}

    public NodeSlot[] slots(){
        return (NodeSlot[])LogicSlotMap.map.get(getClass());
    }

    public abstract NodeCategory category();

    public void build(Table table){}

    public String name(){
        return getClass().getSimpleName().replace("Node", "");
    }

    /** A field for a node, either an input or output. */
    public static class NodeSlot{
        /** The slot's display name. */
        public final String name;
        /** If true this field accepts values. */
        public final boolean input;
        /** The type of data accepted or returned by this slot. */
        public final DataType type;

        public final NumOutput<?> numOutput;
        public final ObjOutput<?, ?> objOutput;

        public NodeSlot(String name, boolean input, DataType type, NumOutput<?> numOutput, ObjOutput<?, ?> objOutput){
            this.name = name;
            this.input = input;
            this.type = type;
            this.numOutput = numOutput;
            this.objOutput = objOutput;
        }
    }

    public interface NumOutput<N>{
        void set(N node, double val);
    }

    public interface ObjOutput<N, T>{
        void set(N node, T val);
    }

    public interface SetNum{
        void set(double val);
    }

    public interface SetObj<T>{
        void set(T val);
    }

    public enum NodeCategory{
        controlFlow(Pal.accentBack),
        operations(Pal.place.cpy().shiftSaturation(-0.4f).mul(0.7f));

        public final Color color;

        NodeCategory(Color color){
            this.color = color;
        }
    }

    /** The types of data a node field can be. */
    public enum DataType{
        /** A double. Used for integer calculations as well. */
        number(Pal.place),
        /** Any type of content, e.g. item. */
        content(Color.cyan),
        /** A building of a tile. */
        building(Pal.items),
        /** A unit on the map. */
        unit(Pal.health),
        /** Control flow (void)*/
        control(Color.white),
        /** Java string */
        string(Color.royal);

        public final Color color;

        DataType(Color color){
            this.color = color;
        }
    }

}
