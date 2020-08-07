package mindustry.logic;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.ui.*;

public class LogicNodes{
    public static class BinaryOpNode extends LogicNode{
        public BinaryOp op = BinaryOp.add;
        @Slot
        public double a, b;
        @Slot
        public transient SetNum result = val -> {};

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
        @Slot(input = true)
        public transient Runnable input = this::run;
        @Slot
        public double condition;
        @Slot
        public transient Runnable yes, no;

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
        @Slot
        public transient SetNum value;
        public double var;

        @Override
        public void build(Table table){
            table.field(var + "", Styles.nodeField, str -> var = parseDouble(str))
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

        static double parseDouble(String s){
            return s.equals("yes") || s.equals("true") ? 1 :
            s.equals("no") || s.equals("false") ? 0 :
            Strings.parseDouble(s, 0);
        }
    }

    public static class SequenceNode extends LogicNode{
        @Slot(input = true)
        public transient Runnable input = this::run;
        @Slot
        public transient Runnable first, second;

        @Override
        public void run(){
            first.run();
            second.run();
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.controlFlow;
        }
    }

    public static class SignalNode extends LogicNode{
        @Slot
        public transient Runnable run;

        @Override
        public void run(){
            run.run();
        }

        @Override
        public NodeCategory category(){
            return NodeCategory.controlFlow;
        }
    }
}
