package mindustry.core;

import arc.*;
import arc.Graphics.*;
import arc.Input.*;
import arc.assets.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.fragments.*;

import static arc.scene.actions.Actions.*;
import static mindustry.Vars.*;

public class UI implements ApplicationListener, Loadable{
    public static PixmapPacker packer;

    public MenuFragment menufrag;
    public HudFragment hudfrag;
    public ChatFragment chatfrag;
    public ScriptConsoleFragment scriptfrag;
    public MinimapFragment minimapfrag;
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
    //public MinimapDialog minimap;
    public SchematicsDialog schematics;
    public ModsDialog mods;
    public ColorPicker picker;

    public Cursor drillCursor, unloadCursor;

    public UI(){
        Fonts.loadFonts();
    }

    @Override
    public void loadAsync(){

    }

    @Override
    public void loadSync(){
        Fonts.outline.getData().markupEnabled = true;
        Fonts.def.getData().markupEnabled = true;
        Fonts.def.setOwnsTexture(false);

        Core.assets.getAll(BitmapFont.class, new Array<>()).each(font -> font.setUseIntegerPositions(true));
        Core.scene = new Scene();
        Core.input.addProcessor(Core.scene);

        Tex.load();
        Icon.load();
        Styles.load();
        Tex.loadStyles();
        Fonts.loadContentIcons();

        Dialog.setShowAction(() -> sequence(alpha(0f), fadeIn(0.1f)));
        Dialog.setHideAction(() -> sequence(fadeOut(0.1f)));

        Tooltips.getInstance().animations = false;

        Core.settings.setErrorHandler(e -> {
            e.printStackTrace();
            Core.app.post(() -> showErrorMessage("Failed to access local storage.\nSettings will not be saved."));
        });

        ClickListener.clicked = () -> Sounds.press.play();

        Colors.put("accent", Pal.accent);
        Colors.put("unlaunched", Color.valueOf("8982ed"));
        Colors.put("highlight", Pal.accent.cpy().lerp(Color.white, 0.3f));
        Colors.put("stat", Pal.stat);

        drillCursor = Core.graphics.newCursor("drill");
        unloadCursor = Core.graphics.newCursor("unload");
    }

    @Override
    public Array<AssetDescriptor> getDependencies(){
        return Array.with(new AssetDescriptor<>(Control.class), new AssetDescriptor<>("outline", BitmapFont.class), new AssetDescriptor<>("default", BitmapFont.class), new AssetDescriptor<>("chat", BitmapFont.class));
    }

    @Override
    public void update(){
        if(disableUI || Core.scene == null) return;

        Core.scene.act();
        Core.scene.draw();

        if(Core.input.keyTap(KeyCode.MOUSE_LEFT) && Core.scene.getKeyboardFocus() instanceof TextField){
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(!(e instanceof TextField)){
                Core.scene.setKeyboardFocus(null);
            }
        }

        //draw overlay for buttons
        if(state.rules.tutorial){
            control.tutorial.draw();
            Draw.flush();
        }
    }

    @Override
    public void init(){
        menuGroup = new WidgetGroup();
        hudGroup = new WidgetGroup();

        menufrag = new MenuFragment();
        hudfrag = new HudFragment();
        chatfrag = new ChatFragment();
        minimapfrag = new MinimapFragment();
        listfrag = new PlayerListFragment();
        loadfrag = new LoadingFragment();
        scriptfrag = new ScriptConsoleFragment();

        picker = new ColorPicker();
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
        mods = new ModsDialog();
        schematics = new SchematicsDialog();

        Group group = Core.scene.root;

        menuGroup.setFillParent(true);
        menuGroup.touchable(Touchable.childrenOnly);
        menuGroup.visible(() -> state.is(State.menu));
        hudGroup.setFillParent(true);
        hudGroup.touchable(Touchable.childrenOnly);
        hudGroup.visible(() -> !state.is(State.menu));

        Core.scene.add(menuGroup);
        Core.scene.add(hudGroup);

        hudfrag.build(hudGroup);
        menufrag.build(menuGroup);
        chatfrag.container().build(hudGroup);
        minimapfrag.build(hudGroup);
        listfrag.build(hudGroup);
        scriptfrag.container().build(hudGroup);
        loadfrag.build(group);
        new FadeInFragment().build(group);
    }

    @Override
    public void resize(int width, int height){
        if(Core.scene == null) return;
        Core.scene.resize(width, height);
        Events.fire(new ResizeEvent());
    }

    @Override
    public void dispose(){
        if(packer != null){
            packer.dispose();
            packer = null;
        }
    }

    public TextureRegionDrawable getIcon(String name){
        if(Icon.icons.containsKey(name)){
            return Icon.icons.get(name);
        }
        return Core.atlas.getDrawable("error");
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

    public void showTextInput(String titleText, String dtext, int textLength, String def, boolean inumeric, Cons<String> confirmed){
        if(mobile){
            Core.input.getTextInput(new TextInput(){{
                this.title = (titleText.startsWith("$") ? Core.bundle.get(titleText.substring(1)) : titleText);
                this.text = def;
                this.numeric = inumeric;
                this.maxLength = textLength;
                this.accepted = confirmed;
            }});
        }else{
            new Dialog(titleText){{
                cont.margin(30).add(dtext).padRight(6f);
                TextFieldFilter filter = inumeric ? TextFieldFilter.digitsOnly : (f, c) -> true;
                TextField field = cont.addField(def, t -> {}).size(330f, 50f).get();
                field.setFilter((f, c) -> field.getText().length() < textLength && filter.acceptChar(f, c));
                buttons.defaults().size(120, 54).pad(4);
                buttons.addButton("$ok", () -> {
                    confirmed.get(field.getText());
                    hide();
                }).disabled(b -> field.getText().isEmpty());
                buttons.addButton("$cancel", this::hide);
                keyDown(KeyCode.ENTER, () -> {
                    String text = field.getText();
                    if(!text.isEmpty()){
                        confirmed.get(text);
                        hide();
                    }
                });
                keyDown(KeyCode.ESCAPE, this::hide);
                keyDown(KeyCode.BACK, this::hide);
                show();
                Core.scene.setKeyboardFocus(field);
                field.setCursorPosition(def.length());
            }};
        }
    }

    public void showTextInput(String title, String text, String def, Cons<String> confirmed){
        showTextInput(title, text, 32, def, confirmed);
    }

    public void showTextInput(String titleText, String text, int textLength, String def, Cons<String> confirmed){
        showTextInput(titleText, text, textLength, def, false, confirmed);
    }

    public void showInfoFade(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(Actions.fadeOut(7f, Interpolation.fade), Actions.remove());
        table.top().add(info).style(Styles.outlineLabel).padTop(10);
        Core.scene.add(table);
    }

    /** Shows a fading label at the top of the screen. */
    public void showInfoToast(String info, float duration){
        Table table = new Table();
        table.setFillParent(true);
        table.touchable(Touchable.disabled);
        table.update(() -> {
            if(state.is(State.menu)) table.remove();
        });
        table.actions(Actions.delay(duration * 0.9f), Actions.fadeOut(duration * 0.1f, Interpolation.fade), Actions.remove());
        table.top().table(Styles.black3, t -> t.margin(4).add(info).style(Styles.outlineLabel)).padTop(10);
        Core.scene.add(table);
    }

    /** Shows a label at some position on the screen. Does not fade. */
    public void showInfoPopup(String info, float duration, int align, int top, int left, int bottom, int right){
        Table table = new Table();
        table.setFillParent(true);
        table.touchable(Touchable.disabled);
        table.update(() -> {
            if(state.is(State.menu)) table.remove();
        });
        table.actions(Actions.delay(duration), Actions.remove());
        table.align(align).table(Styles.black3, t -> t.margin(4).add(info).style(Styles.outlineLabel)).pad(top, left, bottom, right);
        Core.scene.add(table);
    }

    /** Shows a label in the world. This label is behind everything. Does not fade. */
    public void showLabel(String info, float duration, float worldx, float worldy){
        Table table = new Table();
        table.setFillParent(true);
        table.touchable(Touchable.disabled);
        table.update(() -> {
            if(state.is(State.menu)) table.remove();
        });
        table.actions(Actions.delay(duration), Actions.remove());
        table.align(Align.center).table(Styles.black3, t -> t.margin(4).add(info).style(Styles.outlineLabel)).update(t -> {
            Vec2 v = Core.camera.project(worldx, worldy);
            t.setPosition(v.x, v.y, Align.center);
        });
        //make sure it's at the back
        Core.scene.root.addChildAt(0, table);
    }

    public void showInfo(String info){
        new Dialog(""){{
            getCell(cont).growX();
            cont.margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showErrorMessage(String text){
        new Dialog(""){{
            setFillParent(true);
            cont.margin(15f);
            cont.add("$error.title");
            cont.row();
            cont.addImage().width(300f).pad(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add(text).pad(2f).growX().wrap().get().setAlignment(Align.center);
            cont.row();
            cont.addButton("$ok", this::hide).size(120, 50).pad(4);
        }}.show();
    }

    public void showException(Throwable t){
        showException("", t);
    }

    public void showException(String text, Throwable exc){
        loadfrag.hide();
        new Dialog(""){{
            String message = Strings.getFinalMesage(exc);

            setFillParent(true);
            cont.margin(15);
            cont.add("$error.title").colspan(2);
            cont.row();
            cont.addImage().width(300f).pad(2).colspan(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add((text.startsWith("$") ? Core.bundle.get(text.substring(1)) : text) + (message == null ? "" : "\n[lightgray](" + message + ")")).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
            cont.row();

            Collapser col = new Collapser(base -> base.pane(t -> t.margin(14f).add(Strings.parseException(exc, true)).color(Color.lightGray).left()), true);

            cont.addButton("$details", Styles.togglet, col::toggle).size(180f, 50f).checked(b -> !col.isCollapsed()).fillX().right();
            cont.addButton("$ok", this::hide).size(100, 50).fillX().left();
            cont.row();
            cont.add(col).colspan(2).pad(2);
        }}.show();
    }

    public void showExceptions(String text, String... messages){
        loadfrag.hide();
        new Dialog(""){{

            setFillParent(true);
            cont.margin(15);
            cont.add("$error.title").colspan(2);
            cont.row();
            cont.addImage().width(300f).pad(2).colspan(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add(text).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
            cont.row();

            //cont.pane(p -> {
                for(int i = 0; i < messages.length; i += 2){
                    String btext = messages[i];
                    String details = messages[i + 1];
                    Collapser col = new Collapser(base -> base.pane(t -> t.margin(14f).add(details).color(Color.lightGray).left()), true);

                    cont.add(btext).right();
                    cont.addButton("$details", Styles.togglet, col::toggle).size(180f, 50f).checked(b -> !col.isCollapsed()).fillX().left();
                    cont.row();
                    cont.add(col).colspan(2).pad(2);
                    cont.row();
                }
            //}).colspan(2);

            cont.addButton("$ok", this::hide).size(300, 50).fillX().colspan(2);
        }}.show();
    }

    public void showText(String titleText, String text){
        showText(titleText, text, Align.center);
    }

    public void showText(String titleText, String text, int align){
        new Dialog(titleText){{
            cont.row();
            cont.addImage().width(400f).pad(2).colspan(2).height(4f).color(Pal.accent);
            cont.row();
            cont.add(text).width(400f).wrap().get().setAlignment(align, align);
            cont.row();
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showInfoText(String titleText, String text){
        new Dialog(titleText){{
            cont.margin(15).add(text).width(400f).wrap().left().get().setAlignment(Align.left, Align.left);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showSmall(String titleText, String text){
        new Dialog(titleText){{
            cont.margin(10).add(text);
            titleTable.row();
            titleTable.addImage().color(Pal.accent).height(3f).growX().pad(2f);
            buttons.addButton("$ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showConfirm(String title, String text, Runnable confirmed){
        showConfirm(title, text, null, confirmed);
    }

    public void showConfirm(String title, String text, Boolp hide, Runnable confirmed){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.addButton("$cancel", dialog::hide);
        dialog.buttons.addButton("$ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        if(hide != null){
            dialog.update(() -> {
                if(hide.get()){
                    dialog.hide();
                }
            });
        }
        dialog.keyDown(KeyCode.ESCAPE, dialog::hide);
        dialog.keyDown(KeyCode.BACK, dialog::hide);
        dialog.show();
    }

    public void showCustomConfirm(String title, String text, String yes, String no, Runnable confirmed, Runnable denied){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.addButton(no, () -> {
            dialog.hide();
            denied.run();
        });
        dialog.buttons.addButton(yes, () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(KeyCode.ESCAPE, dialog::hide);
        dialog.keyDown(KeyCode.BACK, dialog::hide);
        dialog.show();
    }

    public void showOkText(String title, String text, Runnable confirmed){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.cont.add(text).width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.addButton("$ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.show();
    }

    public String formatAmount(int number){
        if(number >= 1000000){
            return Strings.fixed(number / 1000000f, 1) + "[gray]" + Core.bundle.get("unit.millions") + "[]";
        }else if(number >= 10000){
            return number / 1000 + "[gray]" + Core.bundle.get("unit.thousands") + "[]";
        }else if(number >= 1000){
            return Strings.fixed(number / 1000f, 1) + "[gray]" + Core.bundle.get("unit.thousands") + "[]";
        }else{
            return number + "";
        }
    }
}
