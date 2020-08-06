package mindustry.logic;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

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

    public static class BinaryOpNode extends LogicNode{
        public BinaryOp op = BinaryOp.add;
        @NodeSlotDef
        public double a, b;
        @NodeSlotDef
        public SetNum result = val -> {};

        @Override
        public void build(Table table){
            //TODO replace with dropdown menu
            TextButton[] button = {null};
            button[0] = table.button(op.symbol, Styles.cleart, () -> {
                op = BinaryOp.all[(op.ordinal() + 1) % BinaryOp.all.length];
                button[0].setText(op.symbol);
            }).size(100f, 40f).pad(2f).get();
        }

        @Override
        public void run(){
            result.set(op.function.get(a, b));
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.operations;
        }
    }

    public static class ConditionNode extends LogicNode{
        @NodeSlotDef(input = true)
        public Runnable input = this::run;
        @NodeSlotDef
        public double condition;
        @NodeSlotDef
        public Runnable yes, no;

        @Override
        public void run(){
            if(condition > 0){
                yes.run();
            }else{
                no.run();
            }
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.controlFlow;
        }
    }

    public static class NumberNode extends LogicNode{
        @NodeSlotDef
        public SetNum value;
        public double var;

        @Override
        public void build(Table table){
            table.field(var + "", Styles.nodeField, str -> var = Strings.parseDouble(str, var))
                .valid(Strings::canParsePositiveFloat)
                .size(100f, 40f).pad(2f).color(table.color)
                .update(f -> f.setColor(f.isValid() ? table.color : Color.white));
        }

        @Override
        public void run(){
            value.set(var);
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.controlFlow;
        }
    }

    public static class SignalNode extends LogicNode{
        @NodeSlotDef
        public Runnable run;

        @Override
        public void run(){
            run.run();
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.controlFlow;
        }
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

    static{
        new NodeSlot("a", true, DataType.number, (BinaryOpNode node, double val) -> node.a = val, null);
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
