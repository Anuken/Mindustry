package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.scene.event.*;
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
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

public class MapObjectivesDialog extends BaseDialog{
    private Seq<MapObjective> objectives = new Seq<>();
    private @Nullable MapObjective selectedObjective;
    private Table list = new Table();

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

                            f.add(field.getName() + ": ");

                            var type = field.getType();

                            if(type == String.class){
                                f.field(Reflect.get(objective, field), text -> {
                                    Reflect.set(objective, field, text);
                                });
                            }else if(type == int.class){
                                f.field(Reflect.<Integer>get(objective, field) + "", text -> {
                                    if(Strings.canParseInt(text)){
                                        Reflect.set(objective, field, Strings.parseInt(text));
                                    }
                                }).valid(Strings::canParseInt);
                            }else if(type == float.class){
                                f.field(Reflect.<Float>get(objective, field) + "", text -> {
                                    if(Strings.canParsePositiveFloat(text)){
                                        Reflect.set(objective, field, Strings.parseFloat(text));
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
                            }else if(type == Team.class){ //TODO list of flags
                                f.button(b -> b.image(Tex.whiteui).color(Reflect.<Team>get(objective, field).color).size(iconSmall), () -> {
                                    showTeamSelect(result -> {
                                        Reflect.set(objective, field, result);
                                        setup();
                                    });
                                }).pad(4);
                            }else if(type == String[].class){ //TODO list of flags

                                Table strings = new Table();
                                strings.marginLeft(20f);
                                Runnable[] rebuild = {null};

                                strings.left();

                                float h = 40f;

                                rebuild[0] = () -> {
                                    strings.clear();
                                    strings.left().defaults().padBottom(3f).padTop(3f);
                                    String[] array = Reflect.get(objective, field);

                                    for(int i = 0; i < array.length; i ++){
                                        int fi = i;
                                        var str = array[i];
                                        strings.field(str, result -> {
                                            array[fi] = result;
                                        }).maxTextLength(20).height(h);

                                        strings.button(Icon.cancel, Styles.squarei, () -> {

                                            String[] next = new String[array.length - 1];
                                            System.arraycopy(array, 0, next, 0, fi);
                                            if(fi < array.length - 1){
                                                System.arraycopy(array, fi + 1, next, fi, array.length - 1 - fi);
                                            }
                                            Reflect.set(objective, field, next);

                                            rebuild[0].run();
                                        }).padLeft(4).size(h);

                                        strings.row();
                                    }

                                    strings.button("+ Add", () -> {
                                        String[] next = new String[array.length + 1];
                                        next[array.length] = "";
                                        System.arraycopy(array, 0, next, 0, array.length);
                                        Reflect.set(objective, field, next);

                                        rebuild[0].run();
                                    }).height(h).width(140f).padLeft(-20f).left().row();
                                };

                                rebuild[0].run();

                                f.row();
                                f.add(strings).colspan(2).fill();

                            }else{
                                f.add("[red]UNFINISHED");
                            }

                            f.row();
                        }

                    }).grow();
                }

            }).width(340f).pad(8f).row();
        }
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

                p.image(block == Blocks.air ? Icon.none.getRegion() : block.uiIcon).size(iconMed).pad(3).with(b -> b.addListener(new HandCursorListener()))
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

            dialog.cont.image(Tex.whiteui).size(iconMed).color(team.color).pad(4).with(i -> i.addListener(new HandCursorListener()))
            .tooltip(team.localized()).get().clicked(() -> {
                cons.get(team);
                dialog.hide();
            });
        }

        dialog.closeOnBack();
        dialog.show();
    }
}
