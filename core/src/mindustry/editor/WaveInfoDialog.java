package mindustry.editor;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.*;

public class WaveInfoDialog extends BaseDialog{
    private int displayed = 20;
    Seq<SpawnGroup> groups = new Seq<>();

    private Table table;
    private int start = 0;
    private UnitType lastType = UnitTypes.dagger;
    private float updateTimer, updatePeriod = 1f;
    private WaveGraph graph = new WaveGraph();

    public WaveInfoDialog(MapEditor editor){
        super("@waves.title");

        shown(this::setup);
        hidden(() -> {
            state.rules.spawns = groups;
        });

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
            }
        });

        onResize(this::setup);
        addCloseButton();

        buttons.button("@waves.edit", () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.defaults().size(210f, 64f);
            dialog.cont.button("@waves.copy", () -> {
                ui.showInfoFade("@waves.copied");
                Core.app.setClipboardText(maps.writeWaves(groups));
                dialog.hide();
            }).disabled(b -> groups == null);
            dialog.cont.row();
            dialog.cont.button("@waves.load", () -> {
                try{
                    groups = maps.readWaves(Core.app.getClipboardText());
                    buildGroups();
                }catch(Exception e){
                    e.printStackTrace();
                    ui.showErrorMessage("@waves.invalid");
                }
                dialog.hide();
            }).disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());
            dialog.cont.row();
            dialog.cont.button("@settings.reset", () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                groups = JsonIO.copy(defaultWaves.get());
                buildGroups();
                dialog.hide();
            }));
            dialog.show();
        }).size(270f, 64f);

        buttons.defaults().width(60f);

        buttons.button("<", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(-1);
            }
        });
        buttons.button(">", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                shift(1);
            }
        });

        buttons.button("-", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(-1);
            }
        });
        buttons.button("+", () -> {}).update(t -> {
            if(t.getClickListener().isPressed()){
                view(1);
            }
        });
    }

    void view(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            displayed += amount;
            if(displayed < 5) displayed = 5;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void shift(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            start += amount;
            if(start < 0) start = 0;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? defaultWaves.get() : state.rules.spawns);

        cont.clear();
        cont.stack(new Table(Tex.clear, main -> {
            main.pane(t -> table = t).growX().growY().padRight(8f).get().setScrollingDisabled(true, false);
            main.row();
            main.button("@add", () -> {
                if(groups == null) groups = new Seq<>();
                groups.add(new SpawnGroup(lastType));
                buildGroups();
            }).growX().height(70f);
        }), new Label("@waves.none"){{
            visible(() -> groups.isEmpty());
            this.touchable = Touchable.disabled;
            setWrap(true);
            setAlignment(Align.center, Align.center);
        }}).width(390f).growY();

        cont.add(graph = new WaveGraph()).grow();

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
                    t.button(b -> {
                        b.left();
                        b.image(group.type.icon(mindustry.ui.Cicon.medium)).size(32f).padRight(3);
                        b.add(group.type.localizedName).color(Pal.accent);

                        b.add().growX();

                        b.button(Icon.cancel, () -> {
                            groups.remove(group);
                            table.getCell(t).pad(0f);
                            t.remove();
                            updateWaves();
                        }).pad(-6).size(46f).padRight(-12f);
                    }, () -> showUpdate(group)).height(46f).pad(-6f).padBottom(0f);

                    t.row();
                    t.table(spawns -> {
                        spawns.field("" + (group.begin + 1), TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text)){
                                group.begin = Strings.parseInt(text) - 1;
                                updateWaves();
                            }
                        }).width(100f);
                        spawns.add("@waves.to").padLeft(4).padRight(4);
                        spawns.field(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text)){
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
                        p.add("@waves.every").padRight(4);
                        p.field(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0){
                                group.spacing = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(100f);
                        p.add("@waves.waves").padLeft(4);
                    });

                    t.row();
                    t.table(a -> {
                        a.field(group.unitAmount + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text)){
                                group.unitAmount = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(80f);

                        a.add(" + ");
                        a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                            if(Strings.canParsePositiveFloat(text)){
                                group.unitScaling = 1f / Strings.parseFloat(text);
                                updateWaves();
                            }
                        }).width(80f);
                        a.add("@waves.perspawn").padLeft(4);
                    });
                    t.row();
                    t.table(a -> {
                        a.field((int)group.shields + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text)){
                                group.shields = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(80f);

                        a.add(" + ");
                        a.field((int)group.shieldScaling + "", TextFieldFilter.digitsOnly, text -> {
                            if(Strings.canParsePositiveInt(text)){
                                group.shieldScaling = Strings.parseInt(text);
                                updateWaves();
                            }
                        }).width(80f);
                        a.add("@waves.shields").padLeft(4);
                    });

                    t.row();
                    t.check("@waves.guardian", b -> group.effect = (b ? StatusEffects.boss : null)).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f);
                }).width(340f).pad(8);

                table.row();
            }
        }else{
            table.add("@editor.default");
        }

        updateWaves();
    }

    void showUpdate(SpawnGroup group){
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        dialog.cont.pane(p -> {
            int i = 0;
            for(UnitType type : content.units()){
                if(type.isHidden()) continue;
                p.button(t -> {
                    t.left();
                    t.image(type.icon(Cicon.medium)).size(40f).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    lastType = type;
                    group.type = type;
                    dialog.hide();
                    buildGroups();
                }).pad(2).margin(12f).fillX();
                if(++i % 3 == 0) p.row();
            }
        });
        dialog.show();
    }

    void updateWaves(){
        graph.groups = groups;
        graph.from = start;
        graph.to = start + displayed;
        graph.rebuild();
    }
}
