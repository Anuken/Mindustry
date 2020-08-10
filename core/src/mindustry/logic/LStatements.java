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

import static mindustry.world.blocks.logic.LogicDisplay.*;

public class LStatements{

    //TODO broken
    //@RegisterStatement("#")
    public static class CommentStatement extends LStatement{
        public String comment = "";

        @Override
        public void build(Table table){
            table.area(comment, Styles.nodeArea, v -> comment = v).growX().height(90f).padLeft(2).padRight(6).color(table.color);
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return null;
        }
    }

    @RegisterStatement("noop")
    public static class InvalidStatement extends LStatement{

        @Override
        public void build(Table table){
        }

        @Override
        public LCategory category(){
            return LCategory.operations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new NoopI();
        }
    }

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


    @RegisterStatement("draw")
    public static class DrawStatement extends LStatement{
        public GraphicsType type = GraphicsType.line;
        public String x = "0", y = "0", p1 = "0", p2 = "0", p3 = "0";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.left();

            table.button(b -> {
                b.label(() -> type.name());
                b.clicked(() -> showSelect(b, GraphicsType.all, type, t -> {
                    type = t;
                    rebuild(table);
                }, 2, cell -> cell.size(100, 50)));
            }, Styles.logict, () -> {}).size(90, 40).color(table.color).left().padLeft(2);

            if(type != GraphicsType.stroke){
                table.row();
            }

            table.table(s -> {
                s.left();
                s.setColor(table.color);

                switch(type){
                    case clear:
                    case color:
                        fields(s, "r", x, v -> x = v);
                        fields(s, "g", y, v -> y = v);
                        fields(s, "b", p1, v -> p1 = v);
                        break;
                    case stroke:
                        s.add().width(4);
                        fields(s, x, v -> x = v);
                        break;
                    case line:
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        s.row();
                        fields(s, "x2", p1, v -> p1 = v);
                        fields(s, "y2", p2, v -> p2 = v);
                        break;
                    case rect:
                    case lineRect:
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        s.row();
                        fields(s, "width", p1, v -> p1 = v);
                        fields(s, "height", p2, v -> p2 = v);
                        break;
                    case poly:
                    case linePoly:
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        s.row();
                        fields(s, "sides", p1, v -> p1 = v);
                        fields(s, "radius", p2, v -> p2 = v);
                        s.row();
                        fields(s, "rotation", p3, v -> p3 = v);
                        break;
                }
            }).expand().left();

        }

        @Override
        public LCategory category(){
            return LCategory.io;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new DrawI((byte)type.ordinal(), 0, builder.var(x), builder.var(y), builder.var(p1), builder.var(p2), builder.var(p3));
        }
    }

    @RegisterStatement("drawflush")
    public static class DrawFlushStatement extends LStatement{
        public String target = "@0";

        @Override
        public void build(Table table){
            table.add(" to ");
            field(table, target, str -> target = str);
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new DrawFlushI(builder.var(target));
        }
    }

    @RegisterStatement("print")
    public static class PrintStatement extends LStatement{
        public String value = "\"frog\"";

        @Override
        public void build(Table table){
            field(table, value, str -> value = str).width(0f).growX().padRight(3);
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

    @RegisterStatement("printflush")
    public static class PrintFlushStatement extends LStatement{
        public String target = "@0";

        @Override
        public void build(Table table){
            table.add(" to ");
            field(table, target, str -> target = str);
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new PrintFlushI(builder.var(target));
        }
    }

    @RegisterStatement("control")
    public static class ControlStatement extends LStatement{
        public LAccess type = LAccess.enabled;
        public String target = "@0", p1 = "0", p2 = "0", p3 = "0", p4 = "0";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.left();

            table.add(" set ");

            table.button(b -> {
                b.label(() -> type.name());
                b.clicked(() -> showSelect(b, LAccess.controls, type, t -> {
                    type = t;
                    rebuild(table);
                }, 2, cell -> cell.size(100, 50)));
            }, Styles.logict, () -> {
            }).size(90, 40).color(table.color).left().padLeft(2);

            table.add(" of ");

            field(table, target, v -> target = v);

            table.row();

            //Q: why don't you just use arrays for this?
            //A: arrays aren't as easy to serialize so the code generator doesn't handle them
            int c = 0;
            for(int i = 0; i < type.parameters.length; i++){

                fields(table, type.parameters[i], i == 0 ? p1 : i == 1 ? p2 : i == 2 ? p3 : p4, i == 0 ? v -> p1 = v : i == 1 ? v -> p2 = v : i == 2 ? v -> p3 = v : v -> p4 = v);

                if(++c % 2 == 0) table.row();
            }
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new ControlI(type, builder.var(target), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
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
                            i.left();
                            int c = 0;
                            for(Item item : Vars.content.items()){
                                if(!item.unlockedNow()) continue;
                                i.button(new TextureRegionDrawable(item.icon(Cicon.small)), Styles.cleari, () -> {
                                    stype("@" + item.name);
                                    hide.run();
                                }).size(40f);

                                if(++c % 6 == 0) i.row();
                            }
                        }),
                        //liquids
                        new Table(i -> {
                            i.left();
                            int c = 0;
                            for(Liquid item : Vars.content.liquids()){
                                if(!item.unlockedNow()) continue;
                                i.button(new TextureRegionDrawable(item.icon(Cicon.small)), Styles.cleari, () -> {
                                    stype("@" + item.name);
                                    hide.run();
                                }).size(40f);

                                if(++c % 6 == 0) i.row();
                            }
                        }),
                        //sensors
                        new Table(i -> {
                            for(LAccess sensor : LAccess.senseable){
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
                    t.add(stack).colspan(3).expand().left();
                }));
            }, Styles.logict, () -> {}).size(40f).padLeft(-1).color(table.color);

            table.add(" in ");

            field(table, from, str -> from = str);
        }

        private void stype(String text){
            tfield.setText(text);
            this.type = text;
        }

        @Override
        public LCategory category(){
            return LCategory.blocks;
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
            }, Styles.logict, () -> {}).size(50f, 40f).pad(4f).color(table.color);

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
            }, Styles.logict, () -> {}).size(50f, 40f).pad(3f).color(table.color);

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

    @RegisterStatement("jump")
    public static class JumpStatement extends LStatement{
        public transient StatementElem dest;

        public int destIndex;

        public ConditionOp op = ConditionOp.notEqual;
        public String value = "x", compare = "false";

        @Override
        public void build(Table table){
            table.add("if ").padLeft(4);

            field(table, value, str -> value = str);

            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, ConditionOp.all, op, o -> op = o));
            }, Styles.logict, () -> {}).size(48f, 40f).pad(4f).color(table.color);

            field(table, compare, str -> compare = str);

            table.add().growX();
            table.add(new JumpButton(Color.white, () -> dest, s -> dest = s)).size(30).right().padLeft(-8);
        }

        //elements need separate conversion logic
        @Override
        public void setupUI(){
            if(elem != null && destIndex > 0 && destIndex < elem.parent.getChildren().size){
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
            return new JumpI(op, builder.var(value), builder.var(compare), destIndex);
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
