package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.SettingsDialog.SettingsTable.Setting;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.net.Net;

import static io.anuke.mindustry.Vars.*;

public class SettingsMenuDialog extends SettingsDialog{
    public SettingsTable graphics;
    public SettingsTable game;
    public SettingsTable sound;

    private Table prefs;
    private Table menu;
    private boolean wasPaused;

    public SettingsMenuDialog(){
        setStyle(Core.scene.skin.get("dialog", WindowStyle.class));

        hidden(() -> {
            if(!state.is(State.menu)){
                if(!wasPaused || Net.active())
                    state.set(State.playing);
            }
        });

        shown(() -> {
            if(!state.is(State.menu)){
                wasPaused = state.is(State.paused);
                state.set(State.paused);
            }
        });

        setFillParent(true);
        title.setAlignment(Align.center);
        titleTable.row();
        titleTable.add(new Image("white")).growX().height(3f).pad(4f).get().setColor(Pal.accent);

        cont.clearChildren();
        cont.remove();
        buttons.remove();

        menu = new Table();

        Consumer<SettingsTable> s = table -> {
            table.row();
            table.addImageTextButton("$back", "icon-arrow-left", 10 * 3, this::back).size(240f, 60f).colspan(2).padTop(15f);
        };

        game = new SettingsTable(s);
        graphics = new SettingsTable(s);
        sound = new SettingsTable(s);

        prefs = new Table();
        prefs.top();
        prefs.margin(14f);

        menu.defaults().size(300f, 60f).pad(3f);
        menu.addButton("$settings.game", () -> visible(0));
        menu.row();
        menu.addButton("$settings.graphics", () -> visible(1));
        menu.row();
        menu.addButton("$settings.sound", () -> visible(2));
        if(!Vars.mobile){
            menu.row();
            menu.addButton("$settings.controls", ui.controls::show);
        }
        menu.row();
        menu.addButton("$settings.language", ui.language::show);

        prefs.clearChildren();
        prefs.add(menu);

        ScrollPane pane = new ScrollPane(prefs);
        pane.addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                Element actor = pane.hit(x, y, true);
                if(actor instanceof Slider){
                    pane.setFlickScroll(false);
                    return true;
                }

                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                pane.setFlickScroll(true);
                super.touchUp(event, x, y, pointer, button);
            }
        });
        pane.setFadeScrollBars(false);

        row();
        add(pane).grow().top();
        row();
        add(buttons).fillX();

        hidden(this::back);

        addSettings();
    }

    void addSettings(){
        //TODO add when sound works again
        //sound.volumePrefs();
        sound.add("[LIGHT_GRAY]there is no sound implemented in v4 yet");

        game.screenshakePref();
        game.checkPref("effects", true);
        if(mobile){
            game.checkPref("autotarget", true);
        }
        game.sliderPref("saveinterval", 60, 10, 5 * 120, i -> Core.bundle.format("setting.seconds", i));

        if(!mobile){
            game.checkPref("crashreport", true);
        }

        game.pref(new Setting(){
            @Override
            public void add(SettingsTable table){
                table.addButton("$settings.cleardata", () -> {
                    FloatingDialog dialog = new FloatingDialog("$settings.cleardata");
                    dialog.setFillParent(false);
                    dialog.cont.defaults().size(230f, 60f).pad(3);
                    dialog.addCloseButton();
                    dialog.cont.addButton("$settings.clearunlocks", () -> {
                        ui.showConfirm("$confirm", "$settings.clear.confirm", () -> {
                            data.reset();
                            dialog.hide();
                        });
                    });
                    dialog.cont.row();
                    dialog.cont.addButton("$settings.clearall", () -> {
                        ui.showConfirm("$confirm", "$settings.clearall.confirm", () -> {
                            ObjectMap<String, Object> map = new ObjectMap<>();
                            for(String value : Core.settings.keys()){
                                if(value.contains("usid") || value.contains("uuid")){
                                    map.put(value, Core.settings.getString(value));
                                }
                            }
                            Core.settings.clear();
                            Core.settings.putAll(map);
                            Core.settings.save();

                            for(FileHandle file : dataDirectory.list()){
                                file.deleteDirectory();
                            }

                            Core.app.exit();
                        });
                    });
                    dialog.cont.row();
                    dialog.show();
                }).size(220f, 60f).pad(6).left();
                table.add();
                table.row();
            }
        });

        graphics.sliderPref("fpscap", 125, 5, 125, 5, s -> (s > 120 ? Core.bundle.get("setting.fpscap.none") : Core.bundle.format("setting.fpscap.text", s)));
        graphics.sliderPref("chatopacity", 100, 0, 100, 5, s -> s + "%");

        if(!mobile){
            graphics.checkPref("vsync", true, b -> Core.graphics.setVSync(b));
            graphics.checkPref("fullscreen", false, b -> {
                if(b){
                    Core.graphics.setFullscreenMode(Core.graphics.getDisplayMode());
                }else{
                    Core.graphics.setWindowedMode(600, 480);
                }
            });

            graphics.checkPref("borderlesswindow", false, b -> Core.graphics.setUndecorated(b));

            Core.graphics.setVSync(Core.settings.getBool("vsync"));
            if(Core.settings.getBool("fullscreen")){
                Core.graphics.setFullscreenMode(Core.graphics.getDisplayMode());
            }

            if(Core.settings.getBool("borderlesswindow")){
                Core.graphics.setUndecorated(true);
            }
        }else{
            graphics.checkPref("landscape", false, b -> {
                if(b){
                    Platform.instance.beginForceLandscape();
                }else{
                    Platform.instance.endForceLandscape();
                }
            });

            if(Core.settings.getBool("landscape")){
                Platform.instance.beginForceLandscape();
            }
        }

        graphics.checkPref("playerchat", true);
        graphics.checkPref("minimap", !mobile);
        graphics.checkPref("fps", false);
        graphics.checkPref("indicators", true);
        graphics.checkPref("animatedwater", false);
        graphics.checkPref("animatedshields", !mobile);
        graphics.checkPref("lasers", true);
        graphics.checkPref("pixelate", false);

        //TODO is this necessary?
        /*
        graphics.checkPref("linear", false, b -> {
            for(Texture tex : Core.atlas.getTextures()){
                TextureFilter filter = b ? TextureFilter.Linear : TextureFilter.Nearest;
                tex.setFilter(filter, filter);
            }
        });

        if(Core.settings.getBool("linear")){
            for(Texture tex : Core.atlas.getTextures()){
                TextureFilter filter = TextureFilter.Linear;
                tex.setFilter(filter, filter);
            }
        }*/
    }

    private void back(){
        prefs.clearChildren();
        prefs.add(menu);
    }

    private void visible(int index){
        prefs.clearChildren();
        Table table = new Table[]{game, graphics, sound}[index];
        prefs.add(table);
    }

    @Override
    public void addCloseButton(){
        buttons.addImageTextButton("$menu", "icon-arrow-left", 30f, this::hide).size(230f, 64f);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK)
                hide();
        });
    }
}
