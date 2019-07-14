package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.freetype.FreeTypeFontGenerator;
import io.anuke.arc.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.AtlasRegion;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.TextField.TextFieldFilter;
import io.anuke.arc.scene.ui.Tooltip.Tooltips;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.editor.MapEditorDialog;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.ui.fragments.*;

import static io.anuke.arc.scene.actions.Actions.sequence;
import static io.anuke.mindustry.Vars.*;

public class UI implements ApplicationListener{
    private FreeTypeFontGenerator generator;

    public MenuFragment menufrag;
    public HudFragment hudfrag;
    public ChatFragment chatfrag;
    public PlayerListFragment listfrag;
    public LoadingFragment loadfrag;

    public WidgetGroup menuGroup, hudGroup;

    public AboutDialog about;
    public GameOverDialog restart;
    public CustomGameDialog custom;
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
    public DatabaseDialog database;
    public ContentInfoDialog content;
    public DeployDialog deploy;
    public TechTreeDialog tech;
    public MinimapDialog minimap;

    public Cursor drillCursor, unloadCursor;

    public UI(){
        Skin skin = new Skin(Core.atlas);
        generateFonts(skin);
        loadExtraStyle(skin);
        skin.load(Core.files.internal("sprites/uiskin.json"));

        for(BitmapFont font : skin.getAll(BitmapFont.class).values()){
            font.setUseIntegerPositions(true);
        }

        Core.scene = new Scene(skin);
        Core.input.addProcessor(Core.scene);

        Dialog.setShowAction(() -> sequence());
        Dialog.setHideAction(() -> sequence());

        Tooltips.getInstance().animations = false;

        Core.settings.setErrorHandler(e -> {
            e.printStackTrace();
            Core.app.post(() -> showError("Failed to access local storage.\nSettings will not be saved."));
        });

        Colors.put("accent", Pal.accent);
        Colors.put("stat", Pal.stat);

        loadCursors();
    }

    void loadExtraStyle(Skin skin){
        AtlasRegion region = Core.atlas.find("flat-down-base");
        int[] splits = region.splits;

        ScaledNinePatchDrawable copy = new ScaledNinePatchDrawable(new NinePatch(region, splits[0], splits[1], splits[2], splits[3])){
            public float getLeftWidth(){ return 0; }
            public float getRightWidth(){ return 0; }
            public float getTopHeight(){ return 0; }
            public float getBottomHeight(){ return 0; }
        };
        copy.setMinWidth(0);
        copy.setMinHeight(0);
        copy.setTopHeight(0);
        copy.setRightWidth(0);
        copy.setBottomHeight(0);
        copy.setLeftWidth(0);
        skin.add("flat-down", copy, Drawable.class);
    }

    void loadCursors(){
        int cursorScaling = 1, outlineThickness = 3;
        Color outlineColor = Color.valueOf("444444");

        drillCursor = Core.graphics.newCursor("drill", cursorScaling, outlineColor, outlineThickness);
        unloadCursor = Core.graphics.newCursor("unload", cursorScaling, outlineColor, outlineThickness);
        SystemCursor.arrow.set(Core.graphics.newCursor("cursor", cursorScaling, outlineColor, outlineThickness));
        SystemCursor.hand.set(Core.graphics.newCursor("hand", cursorScaling, outlineColor, outlineThickness));
        SystemCursor.ibeam.set(Core.graphics.newCursor("ibeam", cursorScaling, outlineColor, outlineThickness));

        Core.graphics.restoreCursor();
    }

    void generateFonts(Skin skin){
        generator = new FreeTypeFontGenerator(Core.files.internal("fonts/font.ttf"));
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = (int)(9 * 2 * Math.max(Unit.dp.scl(1f), 0.5f));
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
    }

    @Override
    public void init(){
        menuGroup = new WidgetGroup();
        hudGroup = new WidgetGroup();

        menufrag = new MenuFragment();
        hudfrag = new HudFragment();
        chatfrag = new ChatFragment();
        listfrag = new PlayerListFragment();
        loadfrag = new LoadingFragment();

        editor = new MapEditorDialog();
        controls = new ControlsDialog();
        restart = new GameOverDialog();
        join = new JoinDialog();
        discord = new DiscordDialog();
        load = new LoadDialog();
        custom = new CustomGameDialog();
        language = new LanguageDialog();
        database = new DatabaseDialog();
        settings = new SettingsMenuDialog();
        host = new HostDialog();
        paused = new PausedDialog();
        about = new AboutDialog();
        bans = new BansDialog();
        admins = new AdminsDialog();
        traces = new TraceDialog();
        maps = new MapsDialog();
        content = new ContentInfoDialog();
        deploy = new DeployDialog();
        tech = new TechTreeDialog();
        minimap = new MinimapDialog();

        Group group = Core.scene.root;

        menuGroup.setFillParent(true);
        menuGroup.touchable(Touchable.childrenOnly);
        menuGroup.visible(() -> state.is(State.menu));
        hudGroup.setFillParent(true);
        hudGroup.touchable(Touchable.childrenOnly);
        hudGroup.visible(() -> !state.is(State.menu));

        Core.scene.add(menuGroup);
        Core.scene.add(hudGroup);

        control.input().getFrag().build(hudGroup);
        hudfrag.build(hudGroup);
        menufrag.build(menuGroup);
        chatfrag.container().build(hudGroup);
        listfrag.build(hudGroup);
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

    public void loadAnd(Runnable call){
        loadAnd("$loading", call);
    }

    public void loadAnd(String text, Runnable call){
        loadfrag.show(text);
        Time.runTask(7f, () -> {
            call.run();
            loadfrag.hide();
        });
    }

    public void showTextInput(String titleText, String text, int textLength, String def, TextFieldFilter filter, Consumer<String> confirmed){
        new Dialog(titleText, "dialog"){{
            cont.margin(30).add(text).padRight(6f);
            TextField field = cont.addField(def, t -> {
            }).size(170f, 50f).get();
            field.setFilter((f, c) -> field.getText().length() < textLength && filter.acceptChar(f, c));
            Platform.instance.addDialog(field);
            buttons.defaults().size(120, 54).pad(4);
            buttons.addButton("$ok", () -> {
                confirmed.accept(field.getText());
                hide();
            }).disabled(b -> field.getText().isEmpty());
            buttons.addButton("$cancel", this::hide);
        }}.show();
    }

    public void showTextInput(String title, String text, String def, Consumer<String> confirmed){
        showTextInput(title, text, 12, def, (field, c) -> true, confirmed);
    }

    public void showTextInput(String title, String text, int textLength, String def, Consumer<String> confirmed){
        showTextInput(title, text, textLength < 0 ? 12 : textLength, def, (field, c) -> true, confirmed);
    }

    public void showInfoFade(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(Actions.fadeOut(7f, Interpolation.fade), Actions.remove());
        table.top().add(info).padTop(10);
        Core.scene.add(table);
    }

    public void showInfo(String info){
        new Dialog("", "dialog"){{
            getCell(cont).growX();
            cont.margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showError(String text){
        new Dialog("", "dialog"){{
            setFillParent(true);
            cont.add("$error.title");
            cont.row();
            cont.margin(15).pane(t -> {
                Label l = t.add(text).pad(14f).get();
                l.setAlignment(Align.center, Align.left);
                if(mobile){
                    t.getCell(l).wrap().width(400f);
                }
            });
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showText(String titleText, String text){
        new Dialog(titleText, "dialog"){{
            cont.margin(15).add(text).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showInfoText(String titleText, String text){
        new Dialog(titleText, "dialog"){{
            cont.margin(15).add(text).width(400f).wrap().left().get().setAlignment(Align.left, Align.left);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showConfirm(String title, String text, Runnable confirmed){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.cont.add(text).width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.addButton("$cancel", dialog::hide);
        dialog.buttons.addButton("$ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(KeyCode.ESCAPE, dialog::hide);
        dialog.keyDown(KeyCode.BACK, dialog::hide);
        dialog.show();
    }

    public String formatAmount(int number){
        if(number >= 1000000){
            return Strings.fixed(number / 1000000f, 1) + "[gray]mil[]";
        }else if(number >= 10000){
            return number / 1000 + "[gray]k[]";
        }else if(number >= 1000){
            return Strings.fixed(number / 1000f, 1) + "[gray]k[]";
        }else{
            return number + "";
        }
    }
}
