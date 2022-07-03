package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

public class MapObjectivesDialog extends BaseDialog{
    private static final Seq<String> worldFields = Seq.with("x", "y");

    private Seq<MapObjective> objectives = new Seq<>();
    private Table list = new Table();

    private @Nullable MapObjective selectedObjective;
    private @Nullable ObjectiveMarker selectedMarker;

    public MapObjectivesDialog(){
        super("@editor.objectives");

        buttons.defaults().size(170f, 64f).pad(2f);
        buttons.button("@back", Icon.left, this::hide);

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, in -> {
                    var style = Styles.flatt;

                    in.defaults().size(280f, 60f).left();

                    in.button("@waves.copy", Icon.copy, style, () -> {
                        dialog.hide();

                        Core.app.setClipboardText(JsonIO.write(objectives));
                    }).marginLeft(12f).row();
                    in.button("@waves.load", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            objectives.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));

                            setup();
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null).row();
                    in.button("@clear", Icon.none, style, () -> {
                        dialog.hide();
                        objectives.clear();
                        setup();
                    }).marginLeft(12f).row();
                });
            });

            dialog.addCloseButton();
            dialog.show();
        });

        buttons.button("@add", Icon.add, () -> {
            var selection = new BaseDialog("@add");
            selection.cont.pane(p -> {
                p.background(Tex.button);
                p.marginRight(14);
                p.defaults().size(195f, 56f);
                int i = 0;
                for(var gen : MapObjectives.allObjectiveTypes){
                    var objective = gen.get();

                    p.button(objective.typeName(), Styles.flatt, () -> {
                        objectives.add(objective);
                        setup();
                        selection.hide();
                    }).with(Table::left).get().getLabelCell().growX().left().padLeft(5).labelAlign(Align.left);

                    if(++i % 3 == 0) p.row();
                }
            }).scrollX(false);

            selection.addCloseButton();
            selection.show();
        });

        cont.clear();
        cont.pane(t -> {
            list = t;
            list.top();
        }).grow();
    }

    public void show(Seq<MapObjective> objectives){
        super.show();
        selectedObjective = null;

        this.objectives = objectives;
        setup();
    }

    void setup(){
        list.clear();

        for(var objective : objectives){
            list.table(Tex.button, t -> {
                t.margin(0);

                t.button(b -> {
                    b.left();
                    b.add(objective.typeName()).color(Pal.accent);

                    b.add().growX();

                    b.button(Icon.upOpen, Styles.emptyi, () -> {
                        int index = objectives.indexOf(objective);
                        if(index > 0){
                            objectives.swap(index, index - 1);
                            setup();
                        }
                    }).pad(-6).size(46f);

                    b.button(Icon.downOpen, Styles.emptyi, () -> {
                        int index = objectives.indexOf(objective);
                        if(index < objectives.size - 1){
                            objectives.swap(index, index + 1);
                            setup();
                        }
                    }).pad(-6).size(46f);

                    b.button(Icon.cancel, Styles.emptyi, () -> {
                        objectives.remove(objective);
                        list.getCell(t).pad(0f);

                        t.remove();
                        setup();
                    }).pad(-6).size(46f).padRight(-12f);
                }, () -> {
                    if(selectedObjective != objective){
                        selectedObjective = objective;
                        setup();
                    }
                }).growX().height(46f).pad(-6f).padBottom(0f).row();

                if(selectedObjective == objective){
                    t.table(f -> {
                        f.left();
                        f.margin(10f);

                        f.defaults().minHeight(40f).left();

                        var fields = objective.getClass().getFields();

                        for(var field : fields){
                            if((field.getModifiers() & Modifier.PUBLIC) == 0) continue;

                            displayField(f, field, objective);
                        }

                    }).grow();
                }

            }).width(340f).pad(8f).row();
        }
    }

    void displayField(Table f, Field field, Object objective){
        f.add(field.getName() + ": ");

        var type = field.getType();

        if(type == String.class){
            f.area(Reflect.get(objective, field), text -> {
                Reflect.set(objective, field, text);
            }).height(60f);
        }else if(type == boolean.class){
            f.check("", Reflect.get(objective, field), val -> Reflect.set(objective, field, val));
        }else if(type == int.class){
            f.field(Reflect.<Integer>get(objective, field) + "", text -> {
                if(Strings.canParseInt(text)){
                    Reflect.set(objective, field, Strings.parseInt(text));
                }
            }).valid(Strings::canParseInt);
        }else if(type == float.class){
            float multiplier = worldFields.contains(field.getName()) ? tilesize : 1f;

            f.field((Reflect.<Float>get(objective, field) / multiplier) + "", text -> {
                if(Strings.canParsePositiveFloat(text)){
                    Reflect.set(objective, field, Strings.parseFloat(text) * multiplier);
                }
            }).valid(Strings::canParseFloat);
        }else if(type == UnlockableContent.class){

            f.button(b -> b.image(Reflect.<UnlockableContent>get(objective, field).uiIcon).size(iconSmall), () -> {
                showContentSelect(null, result -> {
                    Reflect.set(objective, field, result);
                    setup();
                }, b -> b.techNode != null);
            }).pad(4);

        }else if(type == Block.class){
            f.button(b -> b.image(Reflect.<Block>get(objective, field).uiIcon).size(iconSmall), () -> {
                showContentSelect(ContentType.block, result -> {
                    Reflect.set(objective, field, result);
                    setup();
                }, b -> ((Block)b).synthetic());
            }).pad(4);
        }else if(type == Item.class){
            f.button(b -> b.image(Reflect.<Item>get(objective, field).uiIcon).size(iconSmall), () -> {
                showContentSelect(ContentType.item, result -> {
                    Reflect.set(objective, field, result);
                    setup();
                }, b -> true);
            }).pad(4);
        }else if(type == Team.class){
            f.button(b -> b.image(Tex.whiteui).color(Reflect.<Team>get(objective, field).color).size(iconSmall), () -> {
                showTeamSelect(result -> {
                    Reflect.set(objective, field, result);
                    setup();
                });
            }).pad(4);
        }else if(type == Color.class){
            Color fieldCol = Reflect.get(objective, field);

            f.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(fieldCol));
                }}).grow();
            }).margin(4).size(50f).get().clicked(() -> ui.picker.show(fieldCol, fieldCol::set));

        }else if(type == String[].class){

            Table strings = new Table();
            strings.marginLeft(20f);
            Runnable[] rebuild = {null};

            strings.left();

            float h = 40f;

            rebuild[0] = () -> {
                strings.clear();
                strings.left().defaults().padBottom(3f).padTop(3f);
                String[] array = Reflect.get(objective, field);

                for(int i = 0; i < array.length; i++){
                    int fi = i;
                    var str = array[i];
                    strings.field(str, result -> {
                        array[fi] = result;
                    }).maxTextLength(20).height(h);

                    strings.button(Icon.cancel, Styles.squarei, () -> {
                        Reflect.set(objective, field, Structs.remove(array, fi));

                        rebuild[0].run();
                    }).padLeft(4).size(h);

                    strings.row();
                }

                strings.button("+ Add", () -> {
                    Reflect.set(objective, field, Structs.add(array, ""));

                    rebuild[0].run();
                }).height(h).width(140f).padLeft(-20f).left().row();
            };

            rebuild[0].run();

            f.row();
            f.add(strings).colspan(2).fill();
        }else if(type == ObjectiveMarker[].class){
            Runnable[] rebuild = {null};

            f.row();

            f.table(t -> {
                t.margin(0).marginLeft(10f);

                rebuild[0] = () -> {
                    t.clear();

                    t.left().defaults().growX().left();

                    ObjectiveMarker[] array = Reflect.get(objective, field);

                    for(var marker : array){
                        t.button(b -> {
                            b.left();
                            b.add(marker.typeName()).color(Pal.accent);

                            b.add().growX();

                            b.button(Icon.upOpen, Styles.emptyi, () -> {
                                int index = Structs.indexOf(array, marker);
                                if(index > 0){
                                    Structs.swap(array, index, index - 1);
                                    rebuild[0].run();
                                }
                            }).pad(-6).size(46f);

                            b.button(Icon.downOpen, Styles.emptyi, () -> {
                                int index = Structs.indexOf(array, marker);
                                if(index < objectives.size - 1){
                                    Structs.swap(array, index, index + 1);
                                    rebuild[0].run();
                                }
                            }).pad(-6).size(46f);

                            b.button(Icon.cancel, Styles.emptyi, () -> {
                                Reflect.set(objective, field, Structs.remove(array, marker));

                                t.getCell(b).pad(0f);
                                b.remove();
                                rebuild[0].run();
                            }).pad(-6).size(46f).padRight(-12f);
                        }, () -> {
                            if(selectedMarker != marker){
                                selectedMarker = marker;
                                rebuild[0].run();
                            }
                        }).width(280f).growX().height(46f).pad(-6f).padBottom(12f).row();

                        if(selectedMarker == marker){
                            t.table(b -> {
                                b.left();
                                b.margin(10f);

                                b.defaults().minHeight(40f).left();

                                var fields = marker.getClass().getFields();

                                for(var disp : fields){
                                    if((disp.getModifiers() & Modifier.TRANSIENT) != 0) continue;

                                    displayField(b, disp, marker);
                                }

                            }).padTop(-12f).grow().row();
                        }
                    }

                    t.button("+ Add", () -> {
                        var selection = new BaseDialog("@add");
                        selection.cont.pane(p -> {
                            p.background(Tex.button);
                            p.marginRight(14);
                            p.defaults().size(195f, 56f);
                            int i = 0;
                            for(var gen : MapObjectives.allMarkerTypes){
                                var marker = gen.get();

                                p.button(marker.typeName(), Styles.flatt, () -> {
                                    Reflect.set(objective, field, Structs.add(Reflect.get(objective, field), marker));
                                    rebuild[0].run();
                                    selection.hide();
                                }).with(Table::left).get().getLabelCell().growX().left().padLeft(5).labelAlign(Align.left);

                                if(++i % 3 == 0) p.row();
                            }
                        }).scrollX(false);

                        selection.addCloseButton();
                        selection.show();
                    }).height(40f).width(140f).left().padLeft(-18f).padTop(-6f).row();
                };

                rebuild[0].run();

            }).width(280f).pad(8f).colspan(2).row();

        }else if(type == byte.class){
            f.table(t -> {
                byte value = Reflect.get(objective, field);
                t.left().defaults().left();
                t.check("background", (value & WorldLabel.flagBackground) != 0, val ->
                    Reflect.set(objective, field, (byte)(val ? value | WorldLabel.flagBackground : value & ~WorldLabel.flagBackground))).padTop(4f).padBottom(4f);
                t.row();
                t.check("outline", (value & WorldLabel.flagOutline) != 0, val ->
                    Reflect.set(objective, field, (byte)(val ? value | WorldLabel.flagOutline : value & ~WorldLabel.flagOutline)));
            });
        }else{
            f.add("[red]UNFINISHED");
        }

        f.row();
    }

    void showContentSelect(@Nullable ContentType type, Cons<UnlockableContent> cons, Boolf<UnlockableContent> check){
        BaseDialog dialog = new BaseDialog("");
        dialog.cont.pane(p -> {
            int i = 0;
            for(var block : (type == null ? Vars.content.blocks().copy().<UnlockableContent>as()
                    .add(Vars.content.items())
                    .add(Vars.content.liquids())
                    .add(Vars.content.units()) :
                Vars.content.getBy(type).<UnlockableContent>as())){

                if(!check.get(block)) continue;

                p.image(block == Blocks.air ? Icon.none.getRegion() : block.uiIcon).size(iconMed).pad(3)
                .with(b -> b.addListener(new HandCursorListener()))
                .tooltip(block.localizedName).get().clicked(() -> {
                    cons.get(block);
                    dialog.hide();
                });
                if(++i % 10 == 0) p.row();
            }
        });

        dialog.closeOnBack();
        dialog.show();
    }

    void showTeamSelect(Cons<Team> cons){
        BaseDialog dialog = new BaseDialog("");
        for(var team : Team.baseTeams){

            dialog.cont.image(Tex.whiteui).size(iconMed).color(team.color).pad(4)
            .with(i -> i.addListener(new HandCursorListener()))
            .tooltip(team.localized()).get().clicked(() -> {
                cons.get(team);
                dialog.hide();
            });
        }

        dialog.closeOnBack();
        dialog.show();
    }
}
