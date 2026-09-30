package mindustry.logic.statements;

import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.logic.LogicCanvas.*;

@RegisterStatement("sensor")
public class SensorStatement extends LogicStatement{
    public String to = "result";
    public String from = "block1", type = "@copper";

    private transient int selected = 0;
    private transient TextField tfield;

    @Override
    public void build(Table table){
        field(table, to, str -> to = str);

        table.add(" = ");

        tfield = field(table, type, str -> type = str).width(LogicCanvas.isCompact() ? 140f : 180f).padRight(0f).get();

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
                new Table(i -> {
                    i.left();
                    int c = 0;
                    for(UnitType item : Vars.content.units()){
                        if(!item.unlockedNow() || item.hidden) continue;
                        i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                            stype("@" + item.name);
                            hide.run();
                        }).size(40f);

                        if(++c % 6 == 0) i.row();
                    }

                    for(Block item : Vars.content.blocks()){
                        if(!item.unlockedNow() || item.isHidden()) continue;
                        i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, iconSmall, () -> {
                            stype("@" + item.name);
                            hide.run();
                        }).size(40f);

                        if(++c % 6 == 0) i.row();
                    }
                }),
                //sensors
                new Table(i -> {
                    boolean currentPrivileged = ui.logic.isShown() && ui.logic.isPrivileged();
                    for(LogicProp sensor : (currentPrivileged ? LogicProp.senseablePrivileged : LogicProp.senseable)){
                        i.button(bundle(sensor), Styles.flatt, () -> {
                            stype("@" + sensor.name());
                            hide.run();
                        }).size(240f, 40f).self(c -> tooltip(c, sensor)).row();
                    }
                })
                };

                Drawable[] icons = {Icon.box, Icon.liquid, Icon.units, Icon.tree};
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
                t.add(stack).colspan(4).width(240f).left();
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-1).color(table.color);

        table.add(bundle("in")).padLeft(6f).padRight(6f).self(this::param);

        field(table, from, str -> from = str);
    }

    private void stype(String text){
        tfield.setText(text);
        this.type = text;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SenseI(builder.var(from), builder.var(to), builder.var(type));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.block;
    }
}
