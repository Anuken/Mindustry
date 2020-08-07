package mindustry.logic;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.logic.LExecutor.*;
import mindustry.logic.LCanvas.*;
import mindustry.ui.*;

public class LStatements{

    public static class AssignStatement extends LStatement{
        public String to = "result";
        public String from = "0";

        @Override
        public void build(Table table){
            table.field(to, Styles.nodeField, str -> to = str)
                .size(100f, 40f).pad(2f).color(table.color);

            table.add(" = ");

            table.field(from, Styles.nodeField, str -> from = str)
                .size(100f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            //TODO internal consts need to start with ___ and not be assignable to
            return new LExecutor.AssignI(builder.var(from), builder.var(to));
        }
    }

    public static class ToggleStatement extends LStatement{
        public String target = "result";
        public String value = "0";

        @Override
        public void build(Table table){
            table.field(target, Styles.nodeField, str -> target = str)
            .size(100f, 40f).pad(2f).color(table.color);

            table.add(" -> ");

            table.field(value, Styles.nodeField, str -> value = str)
            .size(100f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LExecutor.LInstruction build(LAssembler builder){
            return new LExecutor.ToggleI(builder.var(target), builder.var(value));
        }
    }

    public static class OpStatement extends LStatement{
        public BinaryOp op = BinaryOp.add;
        public String a = "a", b = "b", dest = "result";

        @Override
        public void build(Table table){
            table.field(dest, Styles.nodeField, str -> dest = str)
                .size(100f, 40f).pad(2f).color(table.color);

            table.add(" = ");

            table.field(a, Styles.nodeField, str -> a = str)
                .size(90f, 40f).pad(2f).color(table.color);


            TextButton[] button = {null};
            button[0] = table.button(op.symbol, Styles.cleart, () -> {
                op = BinaryOp.all[(op.ordinal() + 1) % BinaryOp.all.length];
                button[0].setText(op.symbol);
            }).size(50f, 30f).pad(4f).get();

            table.field(b, Styles.nodeField, str -> b = str)
                .size(90f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new BinaryOpI(op,builder.var(a), builder.var(b), builder.var(dest));
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }
    }

    public static class EndStatement extends LStatement{
        @Override
        public void build(Table table){

        }

        @Override
        public LInstruction build(LAssembler builder){
            return new EndI();
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }
    }

    public static class PrintStatement extends LStatement{
        public String value = "\"frog\"";

        @Override
        public void build(Table table){
            table.field(value, Styles.nodeField, str -> value = str)
                .size(100f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new PrintI(builder.var(value));
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }
    }

    public static class JumpStatement extends LStatement{
        public transient StatementElem dest;

        public int destIndex;
        public String condition = "true";

        @Override
        public void build(Table table){
            table.add("if ").padLeft(6);
            table.field(condition, Styles.nodeField, str -> condition = str)
                .size(100f, 40f).pad(2f).color(table.color);
            table.add().growX();
            table.add(new JumpButton(Color.white, () -> dest, s -> dest = s)).size(30).right().padRight(-17);
        }

        //elements need separate conversion logic
        @Override
        public void setupUI(){
            if(elem != null){
                dest = (StatementElem)elem.parent.getChildren().get(destIndex);
            }
        }

        @Override
        public void saveUI(){
            if(elem != null){
                destIndex = dest == null ? -1 : dest.parent.getChildren().indexOf(dest);
            }
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new JumpI(builder.var(condition),destIndex);
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }
    }

    public static class FetchBuildStatement extends LStatement{
        public String x = "0", y = "0", dest = "result";

        @Override
        public void build(Table table){
            table.field(dest, Styles.nodeField, str -> dest = str)
            .size(100f, 40f).pad(2f).color(table.color);

            table.add(" = ");

            table.field(x, Styles.nodeField, str -> x = str)
            .size(90f, 40f).pad(2f).color(table.color);

            table.add(", ");

            table.field(y, Styles.nodeField, str -> y = str)
            .size(90f, 40f).pad(2f).color(table.color);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new FetchBuildI(builder.var(dest), builder.var(x), builder.var(y));
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
        }
    }
}
