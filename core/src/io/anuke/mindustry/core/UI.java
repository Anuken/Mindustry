package io.anuke.mindustry.core;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Colors;
import io.anuke.arc.graphics.g2d.BitmapFont;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.Scene;
import io.anuke.arc.scene.Skin;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.ui.Dialog;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.scene.ui.TextField.TextFieldFilter;
import io.anuke.arc.scene.ui.TooltipManager;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.arc.freetype.*;
import io.anuke.mindustry.editor.MapEditorDialog;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.ui.fragments.*;

import static io.anuke.arc.scene.actions.Actions.*;
import static io.anuke.mindustry.Vars.*;

public class UI implements ApplicationListener{
    private FreeTypeFontGenerator generator;

    public final MenuFragment menufrag = new MenuFragment();
    public final HudFragment hudfrag = new HudFragment();
    public final ChatFragment chatfrag = new ChatFragment();
    public final PlayerListFragment listfrag = new PlayerListFragment();
    public final BackgroundFragment backfrag = new BackgroundFragment();
    public final LoadingFragment loadfrag = new LoadingFragment();

    public AboutDialog about;
    public RestartDialog restart;
    public CustomGameDialog levels;
    public MapsDialog maps;
    public LoadDialog load;
    public DiscordDialog discord;
    public JoinDialog join;
    public HostDialog host;
    public PausedDialog paused;
    public SettingsMenuDialog settings;
    public ControlsDialog controls;
    public MapEditorDialog editor;
    public LanguageDialog language;
    public BansDialog bans;
    public AdminsDialog admins;
    public TraceDialog traces;
    public ChangelogDialog changelog;
    public LocalPlayerDialog localplayers;
    public UnlocksDialog unlocks;
    public ContentInfoDialog content;
    public SectorsDialog sectors;
    public MissionDialog missions;

    public Cursor drillCursor, unloadCursor;

    public UI(){
        Skin skin = new Skin(Core.atlas);
        generateFonts();
        skin.load(Core.files.internal("ui/uiskin.json"));

        for(BitmapFont font : skin.getAll(BitmapFont.class).values()){
            font.setUseIntegerPositions(true);
        }

        Core.scene = new Scene(skin);

        Dialog.setShowAction(() -> sequence(
            alpha(0f),
            originCenter(),
            moveToAligned(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 2f, Align.center),
            scaleTo(0.0f, 1f),
            parallel(
                scaleTo(1f, 1f, 0.1f, Interpolation.fade),
                fadeIn(0.1f, Interpolation.fade)
            )
        ));

        Dialog.setHideAction(() -> sequence(
            parallel(
                scaleTo(0.01f, 0.01f, 0.1f, Interpolation.fade),
                fadeOut(0.1f, Interpolation.fade)
            )
        ));

        TooltipManager.getInstance().animations = false;

        Core.settings.setErrorHandler(e -> Time.run(1f, () -> showError("Failed to access local storage.\nSettings will not be saved.")));

        Colors.put("accent", Palette.accent);

        loadCursors();
    }

    void loadCursors(){
        int cursorScaling = 3;
        Color outlineColor = Color.valueOf("444444");

        drillCursor = Core.graphics.newCursor("drill", cursorScaling, outlineColor);
        unloadCursor = Core.graphics.newCursor("unload", cursorScaling, outlineColor);
        SystemCursor.arrow.set(Core.graphics.newCursor("cursor", cursorScaling, outlineColor));
        SystemCursor.hand.set(Core.graphics.newCursor("hand", cursorScaling, outlineColor));
        SystemCursor.ibeam.set(Core.graphics.newCursor("ibeam", cursorScaling, outlineColor));

        Core.graphics.restoreCursor();
    }
    
    void generateFonts(){
        generator = new FreeTypeFontGenerator(Core.files.internal("fonts/pixel.ttf"));
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = (int)(14*2 * Math.max(Unit.dp.scl(1f), 0.5f));
        param.shadowColor = Color.DARK_GRAY;
        param.shadowOffsetY = 2;
        param.incremental = true;

        skin.add("default-font", generator.generateFont(param));
        skin.add("default-font-chat", generator.generateFont(param));
        skin.getFont("default-font").getData().markupEnabled = true;
        skin.getFont("default-font").setOwnsTexture(false);
    }

    @Override
    public void update(){
        if(disableUI) return;

        Core.scene.act();
        Core.scene.draw();
        Core.graphics.batch().flush();
    }

    @Override
    public void init(){
        editor = new MapEditorDialog();
        controls = new ControlsDialog();
        restart = new RestartDialog();
        join = new JoinDialog();
        discord = new DiscordDialog();
        load = new LoadDialog();
        levels = new CustomGameDialog();
        language = new LanguageDialog();
        unlocks = new UnlocksDialog();
        settings = new SettingsMenuDialog();
        host = new HostDialog();
        paused = new PausedDialog();
        changelog = new ChangelogDialog();
        about = new AboutDialog();
        bans = new BansDialog();
        admins = new AdminsDialog();
        traces = new TraceDialog();
        maps = new MapsDialog();
        localplayers = new LocalPlayerDialog();
        content = new ContentInfoDialog();
        sectors = new SectorsDialog();
        missions = new MissionDialog();

        Group group = Core.scene.root;

        backfrag.build(group);
        control.input(0).getFrag().build(group);
        hudfrag.build(group);
        menufrag.build(group);
        chatfrag.container().build(group);
        listfrag.build(group);
        loadfrag.build(group);
    }

    @Override
    public void resize(int width, int height){
        Core.scene.resize(width, height);
        Events.fire(new ResizeEvent());
    }

    @Override
    public void dispose(){
        generator.dispose();
    }

    public void loadGraphics(Runnable call){
        loadGraphics("$text.loading", call);
    }

    public void loadGraphics(String text, Runnable call){
        loadfrag.show(text);
        Time.runTask(7f, () -> {
            call.run();
            loadfrag.hide();
        });
    }

    public void loadLogic(Runnable call){
        loadLogic("$text.loading", call);
    }

    public void loadLogic(String text, Runnable call){
        loadfrag.show(text);
        Time.runTask(7f, () ->
            Core.app.post(() -> {
                call.run();
                loadfrag.hide();
            }));
    }

    public void showTextInput(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
        new Dialog(title, "dialog"){{
            content().margin(30).add(text).padRight(6f);
            TextField field = content().addField(def, t -> {
            }).size(170f, 50f).get();
            field.setTextFieldFilter((f, c) -> field.getText().length() < 12 && filter.acceptChar(f, c));
            Platform.instance.addDialog(field);
            buttons().defaults().size(120, 54).pad(4);
            buttons().addButton("$text.ok", () -> {
                confirmed.accept(field.getText());
                hide();
            }).disabled(b -> field.getText().isEmpty());
            buttons().addButton("$text.cancel", this::hide);
        }}.show();
    }

    public void showTextInput(String title, String text, String def, Consumer<String> confirmed){
        showTextInput(title, text, def, (field, c) -> true, confirmed);
    }

    public void showInfoFade(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(Actions.fadeOut(7f, Interpolation.fade), Actions.removeActor());
        table.top().add(info).padTop(8);
        Core.scene.add(table);
    }

    public void showInfo(String info){
        new Dialog("", "dialog"){{
            getCell(content()).growX();
            content().margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showInfo(String info, Runnable clicked){
        new Dialog("", "dialog"){{
            getCell(content()).growX();
            content().margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", () -> {
                clicked.run();
                hide();
            }).size(90, 50).pad(4);
        }}.show();
    }

    public void showError(String text){
        new Dialog("$text.error.title", "dialog"){{
            content().margin(15).add(text).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showText(String title, String text){
        new Dialog(title, "dialog"){{
            content().margin(15).add(text).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showConfirm(String title, String text, Runnable confirmed){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.content().add(text).width(400f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons().defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons().addButton("$text.cancel", dialog::hide);
        dialog.buttons().addButton("$text.ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(KeyCode.ESCAPE, dialog::hide);
        dialog.keyDown(KeyCode.BACK, dialog::hide);
        dialog.show();
    }

    public String formatAmount(int number){
        if(number >= 1000000){
            return Strings.toFixed(number / 1000000f, 1) + "[gray]mil[]";
        }else if(number >= 10000){
            return number / 1000 + "[gray]k[]";
        }else if(number >= 1000){
            return Strings.toFixed(number / 1000f, 1) + "[gray]k[]";
        }else{
            return number + "";
        }
    }
}
