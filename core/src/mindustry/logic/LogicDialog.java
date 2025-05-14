package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LExecutor.*;
import mindustry.logic.LStatements.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.logic.*;

import java.util.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

public class LogicDialog extends BaseDialog{
    public LCanvas canvas;
    Cons<String> consumer = s -> {};
    boolean privileged;
    @Nullable LExecutor executor;
    GlobalVarsDialog globalsDialog = new GlobalVarsDialog();
    boolean wasRows, wasPortrait, forceRestart;

    public LogicDialog(){
        super("logic");

        clearChildren();

        canvas = new LCanvas();
        shouldPause = true;

        addCloseListener();

        shown(this::setup);
        shown(() -> {
            wasRows = LCanvas.useRows();
            wasPortrait = Core.graphics.isPortrait();
        });
        hidden(() -> consumer.get(canvas.save()));
        onResize(() -> {
            if(wasRows != LCanvas.useRows() || wasPortrait != Core.graphics.isPortrait()){
                setup();
                canvas.rebuild();
                wasPortrait = Core.graphics.isPortrait();
                wasRows = LCanvas.useRows();
            }
        });

        //show add instruction on shift+enter
        keyDown(KeyCode.enter, () -> {
            if(Core.input.shift()){
                showAddDialog();
            }
        });

        add(canvas).grow().name("canvas");

        row();

        add(buttons).growX().name("canvas");
    }

    public static Color typeColor(LVar s, Color color){
        return color.set(
            !s.isobj ? Pal.place :
            s.objval == null ? Color.darkGray :
            s.objval instanceof String ? Pal.ammo :
            s.objval instanceof Content ? Pal.logicOperations :
            s.objval instanceof Building ? Pal.logicBlocks :
            s.objval instanceof Unit ? Pal.logicUnits :
            s.objval instanceof Team ? Pal.logicUnits :
            s.objval instanceof Enum<?> ? Pal.logicIo :
            Color.white
        );
    }

    public static String typeName(LVar s){
        return
            !s.isobj ? "number" :
            s.objval == null ? "null" :
            s.objval instanceof String ? "string" :
            s.objval instanceof Content ? "content" :
            s.objval instanceof Building ? "building" :
            s.objval instanceof Team ? "team" :
            s.objval instanceof Unit ? "unit" :
            s.objval instanceof Enum<?> ? "enum" :
            "unknown";
    }

    private void setup(){
        buttons.clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.flatt;
                    t.defaults().size(280f, 60f).left();

                    if(privileged && executor != null && executor.build != null && !ui.editor.isShown()){
                        t.button("@editor.worldprocessors.editname", Icon.edit, style, () -> {
                            ui.showTextInput("", "@editor.name", LogicBlock.maxNameLength, executor.build.tag == null ? "" : executor.build.tag, tag -> {
                                if(privileged && executor != null && executor.build != null){
                                    executor.build.configure(tag);
                                    //just in case of privilege shenanigans...
                                    executor.build.tag = tag;
                                }
                            });
                            dialog.hide();
                        }).marginLeft(12f).row();
                    }

                    t.button("@clear", Icon.cancel, style, () -> {
                        ui.showConfirm("@logic.clear.confirm", () -> canvas.clearStatements());
                        dialog.hide();
                    }).marginLeft(12f).row();

                    t.button("@schematic.copy", Icon.copy, style, () -> {
                        dialog.hide();
                        Core.app.setClipboardText(canvas.save());
                    }).marginLeft(12f).row();

                    t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText().replace("\r\n", "\n"));
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null).row();

                    t.button("@logic.restart", Icon.refresh, style, () -> {
                        forceRestart = true;
                        dialog.hide();
                        hide();
                    }).marginLeft(12f);

                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        if(Core.graphics.isPortrait()) buttons.row();

        buttons.button("@variables", Icon.menu, () -> {
            //in the editor, it should display the global variables only (the button text is different)
            if(!shouldShowVariables()){
                globalsDialog.show();
                return;
            }

            BaseDialog dialog = new BaseDialog("@variables");
            dialog.hidden(() -> {
                if(!wasPaused && !net.active() && !state.isMenu()){
                    state.set(State.paused);
                }
            });

            dialog.shown(() -> {
                if(!wasPaused && !net.active() && !state.isMenu()){
                    state.set(State.playing);
                }
            });

            dialog.cont.pane(p -> {

                p.margin(10f).marginRight(16f);
                p.table(Tex.button, t -> {
                    t.defaults().fillX().height(45f);
                    for(var s : executor.vars){
                        if(s.constant) continue;

                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(" " + s.name + " ", Styles.outlineLabel){{
                            setColor(Pal.accent);
                        }}).padRight(pad);

                        t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                        t.table(Tex.pane, out -> {
                            float period = 15f;
                            float[] counter = {-1f};
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(140f).wrap().get();
                            label.update(() -> {
                                if(counter[0] < 0 || (counter[0] += Time.delta) >= period){
                                    String text = s.isobj ? PrintI.toString(s.objval) : Math.abs(s.numval - Math.round(s.numval)) < 0.00001 ? Math.round(s.numval) + "" : s.numval + "";
                                    if(!label.textEquals(text)){
                                        label.setText(text);
                                        if(counter[0] >= 0f){
                                            label.actions(Actions.color(Pal.accent), Actions.color(Color.white, 0.2f));
                                        }
                                    }
                                    counter[0] = 0f;
                                }
                            });
                            label.act(1f);
                        }).padRight(pad);

                        t.add(new Image(Tex.whiteui, typeColor(s, new Color()).mul(mul))).update(i -> i.setColor(typeColor(s, i.color).mul(mul))).width(stub);

                        t.stack(new Image(Tex.whiteui, typeColor(s, new Color())){{
                            update(() -> setColor(typeColor(s, color)));
                        }}, new Label(() -> " " + typeName(s) + " "){{
                            setStyle(Styles.outlineLabel);
                        }});

                        t.row();

                        t.add().growX().colspan(6).height(4).row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.buttons.button("@logic.globals", Icon.list, () -> globalsDialog.show()).size(210f, 64f);

            dialog.show();
        }).name("variables").update(b -> {
            if(shouldShowVariables()){
                b.setText("@variables");
            }else{
                b.setText("@logic.globals");
            }
        });

        buttons.button("@add", Icon.add, () -> {
            showAddDialog();
        }).disabled(t -> canvas.statements.getChildren().size >= LExecutor.maxInstructions);

        Core.app.post(canvas::rebuild);
    }

    public boolean shouldShowVariables(){
        return executor != null && executor.vars.length > 0 && !state.isMenu();
    }

    public void showAddDialog(){
        BaseDialog dialog = new BaseDialog("@add");
        dialog.cont.table(table -> {
            String[] searchText = {""};
            Prov[] matched = {null};
            Runnable[] rebuild = {() -> {}};

            table.background(Tex.button);

            table.table(s -> {
                s.image(Icon.zoom).padRight(8);
                var search = s.field(null, text -> {
                    searchText[0] = text;
                    rebuild[0].run();
                }).growX().get();
                search.setMessageText("@players.search");

                //auto add first match on enter key
                if(!mobile){

                    //don't focus on mobile (it may cause issues with a popup keyboard)
                    Core.app.post(search::requestKeyboard);

                    search.keyDown(KeyCode.enter, () -> {
                        if(!searchText[0].isEmpty() && matched[0] != null){
                            canvas.add((LStatement)matched[0].get());
                            dialog.hide();
                        }
                    });
                }
            }).growX().padBottom(4).row();

            table.pane(t -> {
                rebuild[0] = () -> {
                    t.clear();

                    var text = searchText[0].toLowerCase();

                    matched[0] = null;

                    for(Prov<LStatement> prov : LogicIO.allStatements){
                        LStatement example = prov.get();
                        if(example instanceof InvalidStatement || example.hidden() || (example.privileged() && !privileged) || (example.nonPrivileged() && privileged) ||
                            (!text.isEmpty() && !example.name().toLowerCase(Locale.ROOT).contains(text) && !example.typeName().toLowerCase(Locale.ROOT).contains(text))) continue;

                        if(matched[0] == null){
                            matched[0] = prov;
                        }

                        LCategory category = example.category();
                        Table cat = t.find(category.name);
                        if(cat == null){
                            t.table(s -> {
                                if(category.icon != null){
                                    s.image(category.icon, Pal.darkishGray).left().size(15f).padRight(10f);
                                }
                                s.add(category.localized()).color(Pal.darkishGray).left().tooltip(category.description());
                                s.image(Tex.whiteui, Pal.darkishGray).left().height(5f).growX().padLeft(10f);
                            }).growX().pad(5f).padTop(10f);

                            t.row();

                            cat = t.table(c -> {
                                c.top().left();
                            }).name(category.name).top().left().growX().fillY().get();
                            t.row();
                        }

                        TextButtonStyle style = new TextButtonStyle(Styles.flatt);
                        style.fontColor = category.color;
                        style.font = Fonts.outline;

                        cat.button(example.name(), style, () -> {
                            canvas.add(prov.get());
                            dialog.hide();
                        }).size(130f, 50f).self(c -> tooltip(c, "lst." + example.name())).top().left();

                        if(cat.getChildren().size % 3 == 0) cat.row();
                    }
                };

                rebuild[0].run();
            }).grow();
        }).fill().maxHeight(Core.graphics.getHeight() * 0.8f);
        dialog.addCloseButton();
        dialog.show();
    }

    public void show(String code, LExecutor executor, boolean privileged, Cons<String> modified){
        this.executor = executor;
        this.privileged = privileged;
        this.forceRestart = false;
        canvas.statements.clearChildren();
        canvas.rebuild();
        canvas.privileged = privileged;
        try{
            canvas.load(code);
        }catch(Throwable t){
            Log.err(t);
            canvas.load("");
        }
        this.consumer = result -> {
            if(forceRestart || !result.equals(code)){
                modified.get(result);
            }
        };

        show();
    }
}
