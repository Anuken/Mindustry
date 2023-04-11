package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
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
        public LInstruction build(LAssembler builder){
            return new ReadI(builder.var(target), builder.var(address), builder.var(output));
        }

        @Override
        public LCategory category(){
            return LCategory.io;
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
        public LInstruction build(LAssembler builder){
            return new WriteI(builder.var(target), builder.var(address), builder.var(input));
        }

        @Override
        public LCategory category(){
            return LCategory.io;
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
                    case col -> {
                        fields(s, "color", x, v -> x = v).width(144f);
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
        public LInstruction build(LAssembler builder){
            return new DrawI((byte)type.ordinal(), 0, builder.var(x), builder.var(y), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
        }

        @Override
        public LCategory category(){
            return LCategory.io;
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
            return LCategory.io;
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
        public LInstruction build(LAssembler builder){
            return new DrawFlushI(builder.var(target));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
        public LInstruction build(LAssembler builder){
            return new PrintFlushI(builder.var(target));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
        public LInstruction build(LAssembler builder){
            return new GetLinkI(builder.var(output), builder.var(address));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
        public LInstruction build(LAssembler builder){
            return new ControlI(type, builder.var(target), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
        public LInstruction build(LAssembler builder){
            return new RadarI(target1, target2, target3, sort, builder.var(radar), builder.var(sortOrder), builder.var(output));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
                                if(!item.unlockedNow() || item.hidden) continue;
                                i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
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
                                if(!item.unlockedNow() || item.hidden) continue;
                                i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                                    stype("@" + item.name);
                                    hide.run();
                                }).size(40f);

                                if(++c % 6 == 0) i.row();
                            }
                        }),
                        //sensors
                        new Table(i -> {
                            for(LAccess sensor : LAccess.senseable){
                                i.button(sensor.name(), Styles.flatt, () -> {
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

                        t.button(icons[i], Styles.squareTogglei, () -> {
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
        public LInstruction build(LAssembler builder){
            return new SenseI(builder.var(from), builder.var(to), builder.var(type));
        }

        @Override
        public LCategory category(){
            return LCategory.block;
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
        public LInstruction build(LAssembler builder){
            return new SetI(builder.var(from), builder.var(to));
        }

        @Override
        public LCategory category(){
            return LCategory.operation;
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
                            c.color.set(category().color);
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
        public LCategory category(){
            return LCategory.operation;
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
        public LInstruction build(LAssembler builder){
            return new WaitI(builder.var(value));
        }

        @Override
        public LCategory category(){
            return LCategory.control;
        }
    }

    @RegisterStatement("stop")
    public static class StopStatement extends LStatement{

        @Override
        public void build(Table table){
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new StopI();
        }

        @Override
        public LCategory category(){
            return LCategory.control;
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
                b.clicked(() -> showSelect(b, GlobalVars.lookableContent, type, o -> {
                    type = o;
                }));
            }, Styles.logict, () -> {}).size(64f, 40f).pad(4f).color(table.color);

            table.add(" # ");

            fields(table, id, str -> id = str);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new LookupI(builder.var(result), builder.var(id), type);
        }

        @Override
        public LCategory category(){
            return LCategory.operation;
        }
    }

    @RegisterStatement("packcolor")
    public static class PackColorStatement extends LStatement{
        public String result = "result", r = "1", g = "0", b = "0", a = "1";

        @Override
        public void build(Table table){
            fields(table, result, str -> result = str);

            table.add(" = pack ");

            row(table);

            fields(table, r, str -> r = str);
            fields(table, g, str -> g = str);
            fields(table, b, str -> b = str);
            fields(table, a, str -> a = str);
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new PackColorI(builder.var(result), builder.var(r), builder.var(g), builder.var(b), builder.var(a));
        }

        @Override
        public LCategory category(){
            return LCategory.operation;
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

            String name = name();

            //hack way of finding the title label...
            Core.app.post(() -> {
                //must be delayed because parent is added later
                if(table.parent != null){
                    Label title = table.parent.find("statement-name");
                    if(title != null){
                        title.update(() -> title.setText((dest != null ? name + " -> " + dest.index : name)));
                    }
                }
            });

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
        public LCategory category(){
            return LCategory.control;
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
                            if(!item.unlockedNow() || item.isHidden() || !item.logicControllable) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
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
        public LInstruction build(LAssembler builder){
            return new UnitBindI(builder.var(type));
        }

        @Override
        public LCategory category(){
            return LCategory.unit;
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
        public LInstruction build(LAssembler builder){
            return new UnitControlI(type, builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4), builder.var(p5));
        }

        @Override
        public LCategory category(){
            return LCategory.unit;
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
        public LInstruction build(LAssembler builder){
            return new RadarI(target1, target2, target3, sort, LExecutor.varUnit, builder.var(sortOrder), builder.var(output));
        }

        @Override
        public LCategory category(){
            return LCategory.unit;
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
                                        i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
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
        public LInstruction build(LAssembler builder){
            return new UnitLocateI(locate, flag, builder.var(enemy), builder.var(ore), builder.var(outX), builder.var(outY), builder.var(outFound), builder.var(outBuild));
        }

        @Override
        public LCategory category(){
            return LCategory.unit;
        }
    }

    @RegisterStatement("getblock")
    public static class GetBlockStatement extends LStatement{
        public TileLayer layer = TileLayer.block;
        public String result = "result", x = "0", y = "0";

        @Override
        public void build(Table table){
            fields(table, result, str -> result = str);

            table.add(" = get ");

            row(table);

            table.button(b -> {
                b.label(() -> layer.name());
                b.clicked(() -> showSelect(b, TileLayer.all, layer, o -> layer = o));
            }, Styles.logict, () -> {}).size(64f, 40f).pad(4f).color(table.color);

            table.add(" at ");

            fields(table, x, str -> x = str);
            table.add(", ");
            fields(table, y, str -> y = str);
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new GetBlockI(builder.var(x), builder.var(y), builder.var(result), layer);
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("setblock")
    public static class SetBlockStatement extends LStatement{
        public TileLayer layer = TileLayer.block;
        public String block = "@air", x = "0", y = "0", team = "@derelict", rotation = "0";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();
            table.add("set");

            table.button(b -> {
                b.label(() -> layer.name());
                b.clicked(() -> showSelect(b, TileLayer.settable, layer, o -> {
                    layer = o;
                    rebuild(table);
                }));
            }, Styles.logict, () -> {}).size(64f, 40f).pad(4f).color(table.color);

            row(table);

            table.add(" at ");

            fields(table, x, str -> x = str);
            table.add(", ");
            fields(table, y, str -> y = str);

            row(table);

            table.add(" to ");

            fields(table, block, str -> block = str);

            if(layer == TileLayer.block){
                row(table);

                table.add("team ");
                fields(table, team, str -> team = str);

                table.add(" rotation ");
                fields(table, rotation, str -> rotation = str);
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetBlockI(builder.var(x), builder.var(y), builder.var(block), builder.var(team), builder.var(rotation), layer);
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("spawn")
    public static class SpawnUnitStatement extends LStatement{
        public String type = "@dagger", x = "10", y = "10", rotation = "90", team = "@sharded", result = "result";

        @Override
        public void build(Table table){
            fields(table, result, str -> result = str);

            table.add(" = spawn ");
            field(table, type, str -> type = str);

            row(table);

            table.add(" at ");
            fields(table, x, str -> x = str);

            table.add(", ");
            fields(table, y, str -> y = str);

            table.row();

            table.add();

            table.add("team ");
            field(table, team, str -> team = str);

            table.add(" rot ");
            fields(table, rotation, str -> rotation = str).left();
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SpawnUnitI(builder.var(type), builder.var(x), builder.var(y), builder.var(rotation), builder.var(team), builder.var(result));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("status")
    public static class ApplyStatusStatement extends LStatement{
        public boolean clear;
        public String effect = "wet", unit = "unit", duration = "10";

        private static @Nullable String[] statusNames;

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.button(clear ? "clear" : "apply", Styles.logict, () -> {
                clear = !clear;
                rebuild(table);
            }).size(80f, 40f).pad(4f).color(table.color);

            if(statusNames == null){
                statusNames = content.statusEffects().select(s -> !s.isHidden()).map(s -> s.name).toArray(String.class);
            }

            table.button(b -> {
                b.label(() -> effect).grow().wrap().labelAlign(Align.center).center();
                b.clicked(() -> showSelect(b, statusNames, effect, o -> {
                    effect = o;
                }, 2, c -> c.size(120f, 38f)));
            }, Styles.logict, () -> {}).size(120f, 40f).pad(4f).color(table.color);

            //TODO effect select

            table.add(clear ? " from " : " to ");

            row(table);

            fields(table, unit, str -> unit = str);

            if(!clear && !(content.statusEffect(effect) != null && content.statusEffect(effect).permanent)){

                table.add(" for ");

                fields(table, duration, str -> duration = str);

                table.add(" sec");
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new ApplyEffectI(clear, effect, builder.var(unit), builder.var(duration));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("spawnwave")
    public static class SpawnWaveStatement extends LStatement{
        public String x = "10", y = "10", natural = "false";

        @Override
        public void build(Table table){
            table.add("natural ");
            fields(table, natural, str -> natural = str);

            table.add("x ").visible(() -> natural.equals("false"));
            fields(table, x, str -> x = str).visible(() -> natural.equals("false"));

            table.add(" y ").visible(() -> natural.equals("false"));
            fields(table, y, str -> y = str).visible(() -> natural.equals("false"));
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SpawnWaveI(builder.var(natural), builder.var(x), builder.var(y));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("setrule")
    public static class SetRuleStatement extends LStatement{
        public LogicRule rule = LogicRule.waveSpacing;
        public String value = "10", p1 = "0", p2 = "0", p3 = "100", p4 = "100";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.button(b -> {
                b.label(() -> rule.name()).growX().wrap().labelAlign(Align.center);
                b.clicked(() -> showSelect(b, LogicRule.all, rule, o -> {
                    rule = o;
                    rebuild(table);
                }, 2, c -> c.width(150f)));
            }, Styles.logict, () -> {}).size(160f, 40f).margin(5f).pad(4f).color(table.color);

            switch(rule){
                case mapArea -> {
                    table.add(" = ");

                    fields(table, "x", p1, s -> p1 = s);
                    fields(table, "y", p2, s -> p2 = s);
                    row(table);
                    fields(table, "w", p3, s -> p3 = s);
                    fields(table, "h", p4, s -> p4 = s);
                }
                case buildSpeed, unitBuildSpeed, unitCost, unitDamage, blockHealth, blockDamage, rtsMinSquad, rtsMinWeight -> {
                    if(p1.equals("0")){
                        p1 = "@sharded";
                    }

                    fields(table, "of", p1, s -> p1 = s);
                    table.add(" = ");
                    row(table);
                    field(table, value, s -> value = s);
                }
                default -> {
                    table.add(" = ");

                    field(table, value, s -> value = s);
                }
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetRuleI(rule, builder.var(value), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("message")
    public static class FlushMessageStatement extends LStatement{
        public MessageType type = MessageType.announce;
        public String duration = "3";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.button(b -> {
                b.label(() -> type.name()).growX().wrap().labelAlign(Align.center);
                b.clicked(() -> showSelect(b, MessageType.all, type, o -> {
                    type = o;
                    rebuild(table);
                }, 2, c -> c.width(150f)));
            }, Styles.logict, () -> {}).size(160f, 40f).padLeft(2).color(table.color);

            switch(type){
                case announce, toast -> {
                    table.add(" for ");
                    fields(table, duration, str -> duration = str);
                    table.add(" secs ");
                }
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new FlushMessageI(type, builder.var(duration));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("cutscene")
    public static class CutsceneStatement extends LStatement{
        public CutsceneAction action = CutsceneAction.pan;
        public String p1 = "100", p2 = "100", p3 = "0.06", p4 = "0";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            table.button(b -> {
                b.label(() -> action.name()).growX().wrap().labelAlign(Align.center);
                b.clicked(() -> showSelect(b, CutsceneAction.all, action, o -> {
                    action = o;
                    rebuild(table);
                }));
            }, Styles.logict, () -> {}).size(90f, 40f).padLeft(2).color(table.color);

            switch(action){
                case pan -> {
                    table.add(" x ");
                    fields(table, p1, str -> p1 = str);
                    table.add(" y ");
                    fields(table, p2, str -> p2 = str);

                    row(table);

                    table.add(" speed ");
                    fields(table, p3, str -> p3 = str);
                }
                case zoom -> {
                    table.add(" level ");
                    fields(table, p1, str -> p1 = str);
                }
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new CutsceneI(action, builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("explosion")
    public static class ExplosionStatement extends LStatement{
        public String team = "@crux", x = "0", y = "0", radius = "5", damage = "50", air = "true", ground = "true", pierce = "false";

        @Override
        public void build(Table table){
            fields(table, "team", team, str -> team = str);
            fields(table, "x", x, str -> x = str);
            row(table);
            fields(table, "y", y, str -> y = str);
            fields(table, "radius", radius, str -> radius = str);
            table.row();
            fields(table, "damage", damage, str -> damage = str);
            fields(table, "air", air, str -> air = str);
            row(table);
            fields(table, "ground", ground, str -> ground = str);
            fields(table, "pierce", pierce, str -> pierce = str);
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler b){
            return new ExplosionI(b.var(team), b.var(x), b.var(y), b.var(radius), b.var(damage), b.var(air), b.var(ground), b.var(pierce));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("setrate")
    public static class SetRateStatement extends LStatement{
        public String amount = "10";

        @Override
        public void build(Table table){
            fields(table, "ipt = ", amount, str -> amount = str);
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetRateI(builder.var(amount));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("fetch")
    public static class FetchStatement extends LStatement{
        public FetchType type = FetchType.unit;
        public String result = "result", team = "@sharded", index = "0", extra = "@conveyor";

        @Override
        public void build(Table table){
            rebuild(table);
        }

        void rebuild(Table table){
            table.clearChildren();

            fields(table, result, r -> result = r);

            table.add(" = ");

            table.button(b -> {
                b.label(() -> type.name()).growX().wrap().labelAlign(Align.center);
                b.clicked(() -> showSelect(b, FetchType.all, type, o -> {
                    type = o;
                    rebuild(table);
                }, 2, c -> c.width(150f)));
            }, Styles.logict, () -> {}).size(160f, 40f).margin(5f).pad(4f).color(table.color);

            row(table);

            fields(table, "team", team, s -> team = s);

            if(type != FetchType.coreCount && type != FetchType.playerCount && type != FetchType.unitCount && type != FetchType.buildCount){
                table.add(" # ");

                row(table);

                fields(table, index, i -> index = i);
            }

            if(type == FetchType.buildCount || type == FetchType.build){
                row(table);

                fields(table, "block", extra, i -> extra = i);
            }
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new FetchI(type, builder.var(result), builder.var(team), builder.var(extra), builder.var(index));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("getflag")
    public static class GetFlagStatement extends LStatement{
        public String result = "result", flag = "\"flag\"";

        @Override
        public void build(Table table){
            fields(table, result, str -> result = str);

            table.add(" = flag ");

            fields(table, flag, str -> flag = str);
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new GetFlagI(builder.var(result), builder.var(flag));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("setflag")
    public static class SetFlagStatement extends LStatement{
        public String flag = "\"flag\"", value = "true";

        @Override
        public void build(Table table){
            fields(table, flag, str -> flag = str);

            table.add(" = ");

            fields(table, value, str -> value = str);
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetFlagI(builder.var(flag), builder.var(value));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }

    @RegisterStatement("setprop")
    public static class SetPropStatement extends LStatement{
        public String type = "@copper", of = "block1", value = "0";

        private transient int selected = 0;
        private transient TextField tfield;

        @Override
        public void build(Table table){
            table.add(" set ");

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
                            if(item.hidden) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
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
                            if(!item.unlockedNow() || item.hidden) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                                stype("@" + item.name);
                                hide.run();
                            }).size(40f);

                            if(++c % 6 == 0) i.row();
                        }
                    }),
                    //sensors
                    new Table(i -> {
                        for(LAccess property : LAccess.settable){
                            i.button(property.name(), Styles.flatt, () -> {
                                stype("@" + property.name());
                                hide.run();
                            }).size(240f, 40f).self(c -> tooltip(c, property)).row();
                        }
                    })
                    };

                    Drawable[] icons = {Icon.box, Icon.liquid, Icon.tree};
                    Stack stack = new Stack(tables[selected]);
                    ButtonGroup<Button> group = new ButtonGroup<>();

                    for(int i = 0; i < tables.length; i++){
                        int fi = i;

                        t.button(icons[i], Styles.squareTogglei, () -> {
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

            table.add(" of ").self(this::param);

            field(table, of, str -> of = str);

            table.add(" to ");

            field(table, value, str -> value = str);
        }

        private void stype(String text){
            tfield.setText(text);
            this.type = text;
        }

        @Override
        public boolean privileged(){
            return true;
        }

        @Override
        public LInstruction build(LAssembler builder){
            return new SetPropI(builder.var(type), builder.var(of), builder.var(value));
        }

        @Override
        public LCategory category(){
            return LCategory.world;
        }
    }
}
