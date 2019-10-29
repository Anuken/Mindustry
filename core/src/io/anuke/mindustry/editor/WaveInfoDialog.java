package io.anuke.mindustry.editor;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.TextField.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.game.SpawnGroup.never;

public class WaveInfoDialog extends FloatingDialog{
    private final static int displayed = 20;
    private Array<SpawnGroup> groups = new Array<>();

    private Table table, preview;
    private int start = 0;
    private UnitType lastType = UnitTypes.dagger;
    private float updateTimer, updatePeriod = 1f;

    public WaveInfoDialog(MapEditor editor){
        super("$waves.title");

        shown(this::setup);
        hidden(() -> {
            state.rules.spawns = groups;
        });

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                Core.app.post(this::hide);
            }
        });

        addCloseButton();
        buttons.addButton("$waves.edit", () -> {
            FloatingDialog dialog = new FloatingDialog("$waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.defaults().size(210f, 64f);
            dialog.cont.addButton("$waves.copy", () -> {
                ui.showInfoFade("$waves.copied");
                Core.app.setClipboardText(maps.writeWaves(groups));
                dialog.hide();
            }).disabled(b -> groups == null);
            dialog.cont.row();
            dialog.cont.addButton("$waves.load", () -> {
                try{
                    groups = maps.readWaves(Core.app.getClipboardText());
                    buildGroups();
                }catch(Exception e){
                    e.printStackTrace();
                    ui.showErrorMessage("$waves.invalid");
                }
                dialog.hide();
            }).disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());
            dialog.cont.row();
            dialog.cont.addButton("$settings.reset", () -> ui.showConfirm("$confirm", "$settings.clear.confirm", () -> {
                groups = JsonIO.copy(defaultWaves.get());
                buildGroups();
                dialog.hide();
            }));
            dialog.show();
        }).size(270f, 64f);
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? defaultWaves.get() : state.rules.spawns);

        cont.clear();
        cont.stack(new Table(Tex.clear, main -> {
            main.pane(t -> table = t).growX().growY().padRight(8f).get().setScrollingDisabled(true, false);
            main.row();
            main.addButton("$add", () -> {
                if(groups == null) groups = new Array<>();
                groups.add(new SpawnGroup(lastType));
                buildGroups();
            }).growX().height(70f);
        }), new Label("$waves.none"){{
            visible(() -> groups.isEmpty());
            touchable(Touchable.disabled);
            setWrap(true);
            setAlignment(Align.center, Align.center);
        }}).width(390f).growY();

        cont.table(Tex.clear, m -> {
            m.add("$waves.preview").color(Color.lightGray).growX().center().get().setAlignment(Align.center, Align.center);
            m.row();
            m.addButton("-", () -> {
            }).update(t -> {
                if(t.getClickListener().isPressed()){
                    updateTimer += Time.delta();
                    if(updateTimer >= updatePeriod){
                        start = Math.max(start - 1, 0);
                        updateTimer = 0f;
                        updateWaves();
                    }
                }
            }).growX().height(70f);
            m.row();
            m.pane(t -> preview = t).grow().get().setScrollingDisabled(true, true);
            m.row();
            m.addButton("+", () -> {
            }).update(t -> {
                if(t.getClickListener().isPressed()){
                    updateTimer += Time.delta();
                    if(updateTimer >= updatePeriod){
                        start++;
                        updateTimer = 0f;
                        updateWaves();
                    }
                }
            }).growX().height(70f);
        }).growY().width(180f).growY();

        buildGroups();
    }

    void buildGroups(){
        table.clear();
        table.top();
        table.margin(10f);

        if(groups != null){
            for(SpawnGroup group : groups){
                table.table(Tex.button, t -> {
                    t.margin(0).defaults().pad(3).padLeft(5f).growX().left();
                    t.addButton(b -> {
                        b.left();
                        b.addImage(group.type.icon(io.anuke.mindustry.ui.Cicon.medium)).size(32f).padRight(3);
                        b.add(group.type.localizedName).color(Pal.accent);
                    }, () -> showUpdate(group)).pad(-6f).padBottom(0f);

                    t.row();
                    t.table(spawns -> {
                        spawns.addField("" + (group.begin + 1), TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePostiveInt(text)){
                                group.begin = Strings.parseInt(text) - 1;
                                updateWaves();
                            }
                        }).width(100f);
                        spawns.add("$waves.to").padLeft(4).padRight(4);
                        spawns.addField(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePostiveInt(text)){
                                group.end = Strings.parseInt(text) - 1;
                                updateWaves();
                            }else if(text.isEmpty()){
                                group.end = never;
                                updateWaves();
                            }
                        }).width(100f).get().setMessageText(Core.bundle.get("waves.never"));
                    });
                    t.row();
                    t.table(p -> {
                        p.add("$waves.every").padRight(4);
                        p.addField(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePostiveInt(text) && Strings.parseInt(text) > 0){
                                group.spacing = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(100f);
                        p.add("$waves.waves").padLeft(4);
                    });

                    t.row();
                    t.table(a -> {
                        a.addField(group.unitAmount + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePostiveInt(text)){
                                group.unitAmount = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(80f);

                        a.add(" + ");
                        a.addField(Strings.fixed(Math.max((Mathf.isZero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                            if(Strings.canParsePositiveFloat(text)){
                                group.unitScaling = 1f / Strings.parseFloat(text);
                                updateWaves();
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
                        updateWaves();
                    }).growX().pad(-6f).padTop(5);
                }).width(340f).pad(16);
                table.row();
            }
        }else{
            table.add("$editor.default");
        }

        updateWaves();
    }

    void showUpdate(SpawnGroup group){
        FloatingDialog dialog = new FloatingDialog("");
        dialog.setFillParent(false);
        int i = 0;
        for(UnitType type : content.units()){
            dialog.cont.addButton(t -> {
                t.left();
                t.addImage(type.icon(io.anuke.mindustry.ui.Cicon.medium)).size(40f).padRight(2f);
                t.add(type.localizedName);
            }, () -> {
                lastType = type;
                group.type = type;
                dialog.hide();
                buildGroups();
            }).pad(2).margin(12f).fillX();
            if(++i % 3 == 0) dialog.cont.row();
        }
        dialog.show();
    }

    void updateWaves(){
        preview.clear();
        preview.top();

        for(int i = start; i < displayed + start; i++){
            int wave = i;
            preview.table(Tex.underline, table -> {
                table.add((wave + 1) + "").color(Pal.accent).center().colspan(2).get().setAlignment(Align.center, Align.center);
                table.row();

                int[] spawned = new int[Vars.content.getBy(ContentType.unit).size];

                for(SpawnGroup spawn : groups){
                    spawned[spawn.type.id] += spawn.getUnitsSpawned(wave);
                }

                for(int j = 0; j < spawned.length; j++){
                    if(spawned[j] > 0){
                        UnitType type = content.getByID(ContentType.unit, j);
                        table.addImage(type.icon(Cicon.medium)).size(8f * 4f).padRight(4);
                        table.add(spawned[j] + "x").color(Color.lightGray).padRight(6);
                        table.row();
                    }
                }

                if(table.getChildren().size == 1){
                    table.add("$none").color(Pal.remove);
                }
            }).width(110f).pad(2f);

            preview.row();
        }
    }
}
