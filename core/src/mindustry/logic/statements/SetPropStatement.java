package mindustry.logic.statements;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

@RegisterStatement("setprop")
public class SetPropStatement extends LStatement{
    public String type = "@copper", of = "block1", value = "0";

    private transient int selected = 0;
    private transient TextField tfield;

    @Override
    public void build(Table table){
        table.add(bundle("set")).padLeft(6f);

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
                //status effects
                new Table(i -> {
                    i.left();
                    for(StatusEffect status : Vars.content.statusEffects()){
                        if(!status.unlockedNow() || !status.show || status == StatusEffects.none) continue;
                        i.button(status.localizedName, status.uiIcon != Core.atlas.find("error") ? new TextureRegionDrawable(status.uiIcon) : Icon.effect, Styles.flatt, iconSmall, () -> {
                            stype("@status-" + status.name);
                            hide.run();
                        }).size(240f, 40f).marginLeft(5f).row();
                    }
                }),
                //sensors
                new Table(i -> {
                    for(LAccess property : LAccess.settable){
                        i.button(bundle(property), Styles.flatt, () -> {
                            stype("@" + property.name());
                            hide.run();
                        }).size(240f, 40f).self(c -> tooltip(c, property)).row();
                    }
                })
                };

                Drawable[] icons = {Icon.box, Icon.liquid, Icon.effect, Icon.tree};
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
                t.add(stack).colspan(icons.length).width(240f).left();
            }));
        }, Styles.logict, () -> {
        }).size(40f).padLeft(-1).color(table.color);

        table.add(bundle("of")).self(this::param);

        field(table, of, str -> of = str).width(isCompact() ? 170f : 180f);

        table.add(bundle("to"));

        field(table, value, str -> value = str).width(isCompact() ? 170f : 180f);
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
