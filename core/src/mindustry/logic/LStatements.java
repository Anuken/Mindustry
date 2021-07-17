package mindustry.logic;

import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;
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
        public Color color(){
            return Pal.logicControl;
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
        public Color color(){
            return Pal.logicOperations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new NoopI();
        }
    }

    @RegisterStatement("read")
    public static class ReadStatement extends LStatement{
        public String output = "result", target = "cell1", address = "0";

        @Override
        public void build(Table table){
            table.add(" read ");

            field(table, output, str -> output = str);

            table.add(" = ");

            fields(table, target, str -> target = str);

            row(table);

            table.add(" at ");

            field(table, address, str -> address = str);
        }

        @Override
        public Color color(){
            return Pal.logicIo;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new ReadI(builder.var(target), builder.var(address), builder.var(output));
        }
    }

    @RegisterStatement("write")
    public static class WriteStatement extends LStatement{
        public String input = "result", target = "cell1", address = "0";

        @Override
        public void build(Table table){
            table.add(" write ");

            field(table, input, str -> input = str);

            table.add(" to ");

            fields(table, target, str -> target = str);

            row(table);

            table.add(" at ");

            field(table, address, str -> address = str);
        }

        @Override
        public Color color(){
            return Pal.logicIo;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new WriteI(builder.var(target), builder.var(address), builder.var(input));
        }
    }

    @RegisterStatement("draw")
    public static class DrawStatement extends LStatement{
        public GraphicsType type = GraphicsType.clear;
        public String x = "0", y = "0", p1 = "0", p2 = "0", p3 = "0", p4 = "0";

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
                    if(type == GraphicsType.color){
                        p2 = "255";
                    }

                    if(type == GraphicsType.image){
                        p1 = "@copper";
                        p2 = "32";
                        p3 = "0";
                    }
                    rebuild(table);
                }, 2, cell -> cell.size(100, 50)));
            }, Styles.logict, () -> {}).size(90, 40).color(table.color).left().padLeft(2);

            if(type != GraphicsType.stroke){
                row(table);
            }

            table.table(s -> {
                s.left();
                s.setColor(table.color);

                switch(type){
                    case clear -> {
                        fields(s, "r", x, v -> x = v);
                        fields(s, "g", y, v -> y = v);
                        fields(s, "b", p1, v -> p1 = v);
                    }
                    case color -> {
                        fields(s, "r", x, v -> x = v);
                        fields(s, "g", y, v -> y = v);
                        fields(s, "b", p1, v -> p1 = v);
                        row(s);
                        fields(s, "a", p2, v -> p2 = v);
                    }
                    case stroke -> {
                        s.add().width(4);
                        fields(s, x, v -> x = v);
                    }
                    case line -> {
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        row(s);
                        fields(s, "x2", p1, v -> p1 = v);
                        fields(s, "y2", p2, v -> p2 = v);
                    }
                    case rect, lineRect -> {
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        row(s);
                        fields(s, "width", p1, v -> p1 = v);
                        fields(s, "height", p2, v -> p2 = v);
                    }
                    case poly, linePoly -> {
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        row(s);
                        fields(s, "sides", p1, v -> p1 = v);
                        fields(s, "radius", p2, v -> p2 = v);
                        row(s);
                        fields(s, "rotation", p3, v -> p3 = v);
                    }
                    case triangle -> {
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        row(s);
                        fields(s, "x2", p1, v -> p1 = v);
                        fields(s, "y2", p2, v -> p2 = v);
                        row(s);
                        fields(s, "x3", p3, v -> p3 = v);
                        fields(s, "y3", p4, v -> p4 = v);
                    }
                    case image -> {
                        fields(s, "x", x, v -> x = v);
                        fields(s, "y", y, v -> y = v);
                        row(s);
                        fields(s, "image", p1, v -> p1 = v);
                        fields(s, "size", p2, v -> p2 = v);
                        row(s);
                        fields(s, "rotation", p3, v -> p3 = v);
                    }
                }
            }).expand().left();
        }

        @Override
        public void afterRead(){
            //0 constant alpha for colors is not allowed
            if(type == GraphicsType.color && p2.equals("0")){
                p2 = "255";
            }
        }

        @Override
        public Color color(){
            return Pal.logicIo;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new DrawI((byte)type.ordinal(), 0, builder.var(x), builder.var(y), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
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
        public Color color(){
            return Pal.logicIo;
        }
    }

    @RegisterStatement("drawflush")
    public static class DrawFlushStatement extends LStatement{
        public String target = "display1";

        @Override
        public void build(Table table){
            table.add(" to ");
            field(table, target, str -> target = str);
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new DrawFlushI(builder.var(target));
        }
    }

    @RegisterStatement("printflush")
    public static class PrintFlushStatement extends LStatement{
        public String target = "message1";

        @Override
        public void build(Table table){
            table.add(" to ");
            field(table, target, str -> target = str);
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new PrintFlushI(builder.var(target));
        }
    }

    @RegisterStatement("getlink")
    public static class GetLinkStatement extends LStatement{
        public String output = "result", address = "0";

        @Override
        public void build(Table table){
            field(table, output, str -> output = str);

            table.add(" = link# ");

            field(table, address, str -> address = str);
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new GetLinkI(builder.var(output), builder.var(address));
        }
    }

    @RegisterStatement("control")
    public static class ControlStatement extends LStatement{
        public LAccess type = LAccess.enabled;
        public String target = "block1", p1 = "0", p2 = "0", p3 = "0", p4 = "0";

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
            }, Styles.logict, () -> {}).size(90, 40).color(table.color).left().padLeft(2);

            table.add(" of ").self(this::param);

            field(table, target, v -> target = v);

            row(table);

            //Q: why don't you just use arrays for this?
            //A: arrays aren't as easy to serialize so the code generator doesn't handle them
            int c = 0;
            for(int i = 0; i < type.params.length; i++){

                fields(table, type.params[i], i == 0 ? p1 : i == 1 ? p2 : i == 2 ? p3 : p4, i == 0 ? v -> p1 = v : i == 1 ? v -> p2 = v : i == 2 ? v -> p3 = v : v -> p4 = v);

                if(++c % 2 == 0) row(table);
            }
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new ControlI(type, builder.var(target), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
        }
    }

    @RegisterStatement("radar")
    public static class RadarStatement extends LStatement{
        public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
        public RadarSort sort = RadarSort.distance;
        public String radar = "turret1", sortOrder = "1", output = "result";

        @Override
        public void build(Table table){
            table.defaults().left();

            if(buildFrom()){
                table.add(" from ").self(this::param);

                fields(table, radar, v -> radar = v);

                row(table);
            }

            for(int i = 0; i < 3; i++){
                int fi = i;
                Prov<RadarTarget> get = () -> (fi == 0 ? target1 : fi == 1 ? target2 : target3);

                table.add(i == 0 ? " target " : " and ").self(this::param);

                table.button(b -> {
                    b.label(() -> get.get().name());
                    b.clicked(() -> showSelect(b, RadarTarget.all, get.get(), t -> {
                        if(fi == 0) target1 = t; else if(fi == 1) target2 = t; else target3 = t;
                    }, 2, cell -> cell.size(100, 50)));
                }, Styles.logict, () -> {}).size(90, 40).color(table.color).left().padLeft(2);

                if(i == 1){
                    row(table);
                }
            }

            table.add(" order ").self(this::param);

            fields(table, sortOrder, v -> sortOrder = v);

            table.row();

            table.add(" sort ").self(this::param);

            table.button(b -> {
                b.label(() -> sort.name());
                b.clicked(() -> showSelect(b, RadarSort.all, sort, t -> {
                    sort = t;
                }, 2, cell -> cell.size(100, 50)));
            }, Styles.logict, () -> {}).size(90, 40).color(table.color).left().padLeft(2);

            table.add(" output ").self(this::param);

            fields(table, output, v -> output = v);
        }

        public boolean buildFrom(){
            return true;
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new RadarI(target1, target2, target3, sort, builder.var(radar), builder.var(sortOrder), builder.var(output));
        }
    }

    @RegisterStatement("sensor")
    public static class SensorStatement extends LStatement{
        public String to = "result";
        public String from = "block1", type = "@copper";

        private transient int selected = 0;
        private transient TextField tfield;

        @Override
        public void build(Table table){
            field(table, to, str -> to = str);

            table.add(" = ");

            row(table);

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
                                i.button(new TextureRegionDrawable(item.uiIcon), Styles.cleari, iconSmall, () -> {
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
                                i.button(new TextureRegionDrawable(item.uiIcon), Styles.cleari, iconSmall, () -> {
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
                                }).size(240f, 40f).self(c -> tooltip(c, sensor)).row();
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

                            t.parent.parent.pack();
                            t.parent.parent.invalidateHierarchy();
                        }).height(50f).growX().checked(selected == fi).group(group);
                    }
                    t.row();
                    t.add(stack).colspan(3).width(240f).left();
                }));
            }, Styles.logict, () -> {}).size(40f).padLeft(-1).color(table.color);

            table.add(" in ").self(this::param);

            field(table, from, str -> from = str);
        }

        private void stype(String text){
            tfield.setText(text);
            this.type = text;
        }

        @Override
        public Color color(){
            return Pal.logicBlocks;
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
        public Color color(){
            return Pal.logicOperations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetI(builder.var(from), builder.var(to));
        }
    }

    @RegisterStatement("op")
    public static class OperationStatement extends LStatement{
        public LogicOp op = LogicOp.add;
        public String dest = "result", a = "a", b = "b";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            field(table, dest, str -> dest = str);

            table.add(" = ");

            if(op.unary){
                opButton(table, table);

                field(table, a, str -> a = str);
            }else{
                row(table);

                //"function"-type operations have the name at the left and arguments on the right
                if(op.func){
                    if(LCanvas.useRows()){
                        table.left();
                        table.row();
                        table.table(c -> {
                            c.color.set(color());
                            c.left();
                            funcs(c, table);
                        }).colspan(2).left();
                    }else{
                        funcs(table, table);
                    }
                }else{
                    field(table, a, str -> a = str);

                    opButton(table, table);

                    field(table, b, str -> b = str);
                }
            }
        }

        void funcs(Table table, Table parent){
            opButton(table, parent);

            field(table, a, str -> a = str);

            field(table, b, str -> b = str);
        }

        void opButton(Table table, Table parent){
            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, LogicOp.all, op, o -> {
                    op = o;
                    rebuild(parent);
                }));
            }, Styles.logict, () -> {}).size(64f, 40f).pad(4f).color(table.color);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new OpI(op,builder.var(a), builder.var(b), builder.var(dest));
        }

        @Override
        public Color color(){
            return Pal.logicOperations;
        }
    }

    @RegisterStatement("wait")
    public static class WaitStatement extends LStatement{
        public String value = "0.5";

        @Override
        public void build(Table table){
            field(table, value, str -> value = str);
            table.add(" sec");
        }

        @Override
        public Color color(){
            return Pal.logicOperations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new WaitI(builder.var(value));
        }
    }

    @RegisterStatement("lookup")
    public static class LookupStatement extends LStatement{
        public ContentType type = ContentType.item;
        public String result = "result", id = "0";

        @Override
        public void build(Table table){
            fields(table, result, str -> result = str);

            table.add(" = lookup ");

            row(table);

            table.button(b -> {
                b.label(() -> type.name());
                b.clicked(() -> showSelect(b, GlobalConstants.lookableContent, type, o -> {
                    type = o;
                }));
            }, Styles.logict, () -> {}).size(64f, 40f).pad(4f).color(table.color);

            table.add(" # ");

            fields(table, id, str -> id = str);
        }

        @Override
        public Color color(){
            return Pal.logicOperations;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new LookupI(builder.var(result), builder.var(id), type);
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
        public Color color(){
            return Pal.logicControl;
        }
    }

    @RegisterStatement("jump")
    public static class JumpStatement extends LStatement{
        private static Color last = new Color();

        public transient StatementElem dest;

        public int destIndex;

        public ConditionOp op = ConditionOp.notEqual;
        public String value = "x", compare = "false";

        @Override
        public void build(Table table){
            table.add("if ").padLeft(4);

            last = table.color;
            table.table(this::rebuild);

            table.add().growX();
            table.add(new JumpButton(() -> dest, s -> dest = s)).size(30).right().padLeft(-8);
        }

        void rebuild(Table table){
            table.clearChildren();
            table.setColor(last);

            if(op != ConditionOp.always) field(table, value, str -> value = str);

            table.button(b -> {
                b.label(() -> op.symbol);
                b.clicked(() -> showSelect(b, ConditionOp.all, op, o -> {
                    op = o;
                    rebuild(table);
                }));
            }, Styles.logict, () -> {}).size(op == ConditionOp.always ? 80f : 48f, 40f).pad(4f).color(table.color);

            if(op != ConditionOp.always) field(table, compare, str -> compare = str);
        }

        //elements need separate conversion logic
        @Override
        public void setupUI(){
            if(elem != null && destIndex >= 0 && destIndex < elem.parent.getChildren().size){
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
        public Color color(){
            return Pal.logicControl;
        }
    }

    @RegisterStatement("ubind")
    public static class UnitBindStatement extends LStatement{
        public String type = "@poly";

        @Override
        public void build(Table table){
            table.add(" type ");

            TextField field = field(table, type, str -> type = str).get();

            table.button(b -> {
                b.image(Icon.pencilSmall);
                b.clicked(() -> showSelectTable(b, (t, hide) -> {
                    t.row();
                    t.table(i -> {
                        i.left();
                        int c = 0;
                        for(UnitType item : Vars.content.units()){
                            if(!item.unlockedNow() || item.isHidden()) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.cleari, iconSmall, () -> {
                                type = "@" + item.name;
                                field.setText(type);
                                hide.run();
                            }).size(40f);

                            if(++c % 6 == 0) i.row();
                        }
                    }).colspan(3).width(240f).left();
                }));
            }, Styles.logict, () -> {}).size(40f).padLeft(-2).color(table.color);
        }

        @Override
        public Color color(){
            return Pal.logicUnits;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new UnitBindI(builder.var(type));
        }
    }

    @RegisterStatement("ucontrol")
    public static class UnitControlStatement extends LStatement{
        public LUnitControl type = LUnitControl.move;
        public String p1 = "0", p2 = "0", p3 = "0", p4 = "0", p5 = "0";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.left();

            table.add(" ");

            table.button(b -> {
                b.label(() -> type.name());
                b.clicked(() -> showSelect(b, LUnitControl.all, type, t -> {
                    if(t == LUnitControl.build && !Vars.state.rules.logicUnitBuild){
                        Vars.ui.showInfo("@logic.nounitbuild");
                    }else{
                        type = t;
                    }
                    rebuild(table);
                }, 2, cell -> cell.size(120, 50)));
            }, Styles.logict, () -> {}).size(120, 40).color(table.color).left().padLeft(2);

            row(table);

            //Q: why don't you just use arrays for this?
            //A: arrays aren't as easy to serialize so the code generator doesn't handle them
            int c = 0;
            for(int i = 0; i < type.params.length; i++){

                fields(table, type.params[i], i == 0 ? p1 : i == 1 ? p2 : i == 2 ? p3 : i == 3 ? p4 : p5, i == 0 ? v -> p1 = v : i == 1 ? v -> p2 = v : i == 2 ? v -> p3 = v : i == 3 ? v -> p4 = v : v -> p5 = v).width(100f);

                if(++c % 2 == 0) row(table);

                if(i == 3){
                    table.row();
                }
            }
        }

        @Override
        public Color color(){
            return Pal.logicUnits;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new UnitControlI(type, builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4), builder.var(p5));
        }
    }

    @RegisterStatement("uradar")
    public static class UnitRadarStatement extends RadarStatement{

        public UnitRadarStatement(){
            radar = "0";
        }

        @Override
        public boolean buildFrom(){
            //do not build the "from" section
            return false;
        }

        @Override
        public Color color(){
            return Pal.logicUnits;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new RadarI(target1, target2, target3, sort, LExecutor.varUnit, builder.var(sortOrder), builder.var(output));
        }
    }

    @RegisterStatement("ulocate")
    public static class UnitLocateStatement extends LStatement{
        public LLocate locate = LLocate.building;
        public BlockFlag flag = BlockFlag.core;
        public String enemy = "true", ore = "@copper";
        public String outX = "outx", outY = "outy", outFound = "found", outBuild = "building";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.add(" find ").left().self(this::param);

            table.button(b -> {
                b.label(() -> locate.name());
                b.clicked(() -> showSelect(b, LLocate.all, locate, t -> {
                    locate = t;
                    rebuild(table);
                }, 2, cell -> cell.size(110, 50)));
            }, Styles.logict, () -> {}).size(110, 40).color(table.color).left().padLeft(2);

            switch(locate){
                case building -> {
                    row(table);
                    table.add(" group ").left().self(this::param);
                    table.button(b -> {
                        b.label(() -> flag.name());
                        b.clicked(() -> showSelect(b, BlockFlag.allLogic, flag, t -> flag = t, 2, cell -> cell.size(110, 50)));
                    }, Styles.logict, () -> {}).size(110, 40).color(table.color).left().padLeft(2);
                    row(table);

                    table.add(" enemy ").left().self(this::param);

                    fields(table, enemy, str -> enemy = str);

                    table.row();
                }

                case ore -> {
                    table.add(" ore ").left().self(this::param);
                    table.table(ts -> {
                        ts.color.set(table.color);

                        fields(ts, ore, str -> ore = str);

                        ts.button(b -> {
                            b.image(Icon.pencilSmall);
                            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                                t.row();
                                t.table(i -> {
                                    i.left();
                                    int c = 0;
                                    for(Item item : Vars.content.items()){
                                        if(!item.unlockedNow()) continue;
                                        i.button(new TextureRegionDrawable(item.uiIcon), Styles.cleari, iconSmall, () -> {
                                            ore = "@" + item.name;
                                            rebuild(table);
                                            hide.run();
                                        }).size(40f);

                                        if(++c % 6 == 0) i.row();
                                    }
                                }).colspan(3).width(240f).left();
                            }));
                        }, Styles.logict, () -> {}).size(40f).padLeft(-2).color(table.color);
                    });


                    table.row();
                }

                case spawn, damaged -> {
                    table.row();
                }
            }

            table.add(" outX ").left().self(this::param);
            fields(table, outX, str -> outX = str);

            table.add(" outY ").left().self(this::param);
            fields(table, outY, str -> outY = str);

            row(table);

            table.add(" found ").left().self(this::param);
            fields(table, outFound, str -> outFound = str);

            if(locate != LLocate.ore){
                table.add(" building ").left().self(this::param);
                fields(table, outBuild, str -> outBuild = str);
            }

        }

        @Override
        public Color color(){
            return Pal.logicUnits;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new UnitLocateI(locate, flag, builder.var(enemy), builder.var(ore), builder.var(outX), builder.var(outY), builder.var(outFound), builder.var(outBuild));
        }
    }
}
