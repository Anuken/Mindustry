package mindustry.logic;

import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.ui.*;

public class LStatements{

    @RegisterStatement("write")
    public static class WriteStatement extends LStatement{
        public String to = "0";
        public String from = "result";

        @Override
        public void build(Table table){
            field(table, to, str -> to = str);

            table.add(" = ");

            field(table, from, str -> from = str);
        }

        @Override
        public LCategory category(){
            return LCategory.io;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new WriteI(builder.var(from), builder.var(to));
        }
    }

    @RegisterStatement("read")
    public static class ReadStatement extends LStatement{
        public String to = "result";
        public String from = "0";

        @Override
        public void build(Table table){
            field(table, to, str -> to = str);

            table.add(" = mem:: ");

            field(table, from, str -> from = str);
        }

        @Override
        public LCategory category(){
            return LCategory.io;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new ReadI(builder.var(from), builder.var(to));
        }
    }

    @RegisterStatement("sensor")
    public static class SensorStatement extends LStatement{
        public String to = "result";
        public String from = "@0", type = "@copper";

        private transient int selected = 0;
        private transient TextField tfield;

        @Override
        public void build(Table table){
            field(table, to, str -> to = str);

            table.add(" = ");

            table.row();

            tfield = field(table, type, str -> type = str).padRight(0f).get();

            table.button(b -> {
                b.image(Icon.pencilSmall);
                //240
                b.clicked(() -> showSelectTable(b, (t, hide) -> {
                    Table[] tables = {
                        //items
                        new Table(i -> {
                            int c = 0;
                            for(Item item : Vars.content.items()){
                                i.button(new TextureRegionDrawable(item.icon(Cicon.small)), Styles.cleari, () -> {
                                    stype("@" + item.name);
                                    hide.run();
                                }).size(40f);

                                if(++c % 6 == 0) i.row();
                            }
                        }),
                        //liquids
                        new Table(i -> {
                            int c = 0;
                            for(Liquid item : Vars.content.liquids()){
                                i.button(new TextureRegionDrawable(item.icon(Cicon.small)), Styles.cleari, () -> {
                                    stype("@" + item.name);
                                    hide.run();
                                }).size(40f);

                                if(++c % 6 == 0) i.row();
                            }
                        }),
                        //sensors
                        new Table(i -> {
                            for(LSensor sensor : LSensor.all){
                                i.button(sensor.name(), Styles.cleart, () -> {
                                    stype("@" + sensor.name());
                                    hide.run();
                                }).size(240f, 40f).row();
                            }
                        })
                    };

                    Drawable[] icons = {Icon.box, Icon.liquid, Icon.tree};
                    Stack stack = new Stack(tables[selected]);
                    ButtonGroup<Button> group = new ButtonGroup<>();

                    for(int i = 0; i < tables.length; i++){
                        int fi = i;

                        t.button(icons[i], Styles.clearTogglei, () -> {
                            selected = fi;

                            stack.clearChildren();
                            stack.addChild(tables[selected]);
                            t.pack();
                        }).size(80f, 50f).checked(selected == fi).group(group);
                    }
                    t.row();
                    t.add(stack).colspan(3);
                }));
            }, Styles.cleart, () -> {}).size(40f).padLeft(-1);

            table.add(" in ");

            field(table, from, str -> from = str);
        }

        private void stype(String text){
            tfield.setText(text);
            this.type = text;
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SenseI(builder.var(from), builder.var(to), builder.var(type));
        }
    }

    @RegisterStatement("set")
    public static class SetStatement extends LStatement{
        public String to = "result";
        public String from = "0";

        @Override
        public void build(Table table){
            field(table, to, str -> to = str);

            table.add(" = ");

            field(table, from, str -> from = str);
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetI(builder.var(from), builder.var(to));
        }
    }

    @RegisterStatement("enable")
    public static class EnableStatement extends LStatement{
        public String target = "result";
        public String value = "0";

        @Override
        public void build(Table table){
            field(table, target, str -> target = str);

            table.add(" -> ");

            field(table, value, str -> value = str);
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new EnableI(builder.var(target), builder.var(value));
        }
    }

    @RegisterStatement("bop")
    public static class BinaryOpStatement extends LStatement{
        public BinaryOp op = BinaryOp.add;
        public String a = "a", b = "b", dest = "result";

        @Override
        public void build(Table table){
            field(table, dest, str -> dest = str);

            table.add(" = ");

            table.row();

            field(table, a, str -> a = str);

            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, BinaryOp.all, op, o -> op = o));
            }, Styles.cleart, () -> {}).size(50f, 40f).pad(4f);

            field(table, b, str -> b = str);
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

    @RegisterStatement("uop")
    public static class UnaryOpStatement extends LStatement{
        public UnaryOp op = UnaryOp.negate;
        public String value = "b", dest = "result";

        @Override
        public void build(Table table){
            field(table, dest, str -> dest = str);

            table.add(" = ");

            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, UnaryOp.all, op, o -> op = o));
            }, Styles.cleart, () -> {}).size(50f, 40f).pad(3f);

            field(table, value, str -> value = str);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new UnaryOpI(op, builder.var(value), builder.var(dest));
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }
    }

    @RegisterStatement("end")
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

    @RegisterStatement("print")
    public static class PrintStatement extends LStatement{
        public String value = "\"frog\"";
        public String target = "result";

        @Override
        public void build(Table table){
            field(table, value, str -> value = str);

            table.add(" to ");

            field(table, target, str -> target = str);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new PrintI(builder.var(value), builder.var(target));
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }
    }

    @RegisterStatement("jump")
    public static class JumpStatement extends LStatement{
        public transient StatementElem dest;

        public int destIndex;
        public String condition = "true";

        @Override
        public void build(Table table){
            table.add("if ").padLeft(6);
            field(table, condition, str -> condition = str);

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

    //disabled until further notice - bypasses the network
    /*
    @RegisterStatement("getbuild")
    public static class getBuildStatement extends LStatement{
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
            return new GetBuildI(builder.var(dest), builder.var(x), builder.var(y));
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
        }
    }*/
}
