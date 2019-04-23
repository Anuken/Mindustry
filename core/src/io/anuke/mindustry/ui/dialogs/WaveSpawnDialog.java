package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.UnitType;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.logic;

public class WaveSpawnDialog extends FloatingDialog{
    private Table table;
    private Array<SpawnGroup> groups;
    private UnitType lastType = UnitTypes.dagger;

    public WaveSpawnDialog(){
        super("$sandbox.waves.title");
        groups = new Array<>();

        setFillParent(true);
        shown(this::setup);
        onResize(this::setup);
        addCloseButton();
    }

    void setup(){
        cont.clear();
        cont.pane(m->table=m);
        table.top();
        table.margin(10f);
        for(SpawnGroup group : groups) {
            table.table("clear", t->{
                t.margin(6f).defaults().pad(2).padLeft(5f).growX().left();
                t.addButton(b -> {
                    b.left();
                    b.addImage(group.type.iconRegion).size(30f).padRight(3);
                    b.add(group.type.localizedName).color(Pal.accent); }, () -> showUpdate(group)).pad(-6f).padBottom(0f);
                t.row();
                t.table(a -> {
                    a.addField(group.unitAmount + "", TextField.TextFieldFilter.digitsOnly, text -> {
                        if(Strings.canParsePostiveInt(text)){
                            group.unitAmount = Strings.parseInt(text);
                            setup();
                        }
                    }).width(80f);
                    a.add(" + ");
                    a.addField(Strings.fixed(Math.max((Mathf.isZero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextField.TextFieldFilter.floatsOnly, text -> {
                        if(Strings.canParsePositiveFloat(text)){
                            group.unitScaling = 1f / Strings.parseFloat(text);
                            setup();
                        }
                    }).width(80f);
                    a.add("$waves.perspawn").padLeft(4);
                });

                t.row();
                t.addCheck("$waves.boss", b -> group.effect = (b ? StatusEffects.boss : null)).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss));

                t.row();
                t.addButton("$waves.remove", () -> {
                    groups.remove(group);
                    table.getCell(t).pad(0f);
                    t.remove();
                    setup();
                }).growX().pad(-6f).padTop(5);
            }).width(340f).pad(5);
            table.row();
        }
        table.addImage("white").growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent).padTop(25).visible(()->groups.size!=0);
        table.row();
        table.addButton("$add", ()->{
            groups.add(new SpawnGroup(lastType));
            setup();
        }).width(400f).padTop(25f);
        table.row();
        table.addButton("$sandbox.waves.run", ()->{
            logic.runTestWave(groups);
            this.hide();
        }).width(200f).padTop(5).update(a -> a.setDisabled(groups.size==0));
    }

    void showUpdate(SpawnGroup group){
        FloatingDialog dialog = new FloatingDialog("");
        dialog.setFillParent(false);
        int i = 0;
        for(UnitType type : content.units()){
            dialog.cont.addButton(t -> {
                t.left();
                t.addImage(type.iconRegion).size(40f).padRight(2f);
                t.add(type.localizedName);
            }, () -> {
                lastType = type;
                group.type = type;
                dialog.hide();
                setup();
            }).pad(2).margin(12f).fillX();
            if(++i % 3 == 0) dialog.cont.row();
        }
        dialog.show();
    }
}
