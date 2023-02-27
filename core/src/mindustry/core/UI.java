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
import mindustry.editor.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.fragments.*;

import static arc.scene.actions.Actions.*;
import static mindustry.Vars.*;

public class UI implements ApplicationListener, Loadable{
    public static String billions, millions, thousands;

    public static PixmapPacker packer;

    public MenuFragment menufrag;
    public HudFragment hudfrag;
    public ChatFragment chatfrag;
    public ConsoleFragment consolefrag;
    public MinimapFragment minimapfrag;
    public PlayerListFragment listfrag;
    public LoadingFragment loadfrag;
    public HintsFragment hints;

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
    public KeybindDialog controls;
    public MapEditorDialog editor;
    public LanguageDialog language;
    public BansDialog bans;
    public AdminsDialog admins;
    public TraceDialog traces;
    public DatabaseDialog database;
    public ContentInfoDialog content;
    public PlanetDialog planet;
    public ResearchDialog research;
    public SchematicsDialog schematics;
    public ModsDialog mods;
    public ColorPicker picker;
    public LogicDialog logic;
    public FullTextDialog fullText;
    public CampaignCompleteDialog campaignComplete;

    public Cursor drillCursor, unloadCursor, targetCursor;

    private @Nullable Element lastAnnouncement;

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

        Core.assets.getAll(Font.class, new Seq<>()).each(font -> font.setUseIntegerPositions(true));
        Core.scene = new Scene();
        Core.input.addProcessor(Core.scene);

        int[] insets = Core.graphics.getSafeInsets();
        Core.scene.marginLeft = insets[0];
        Core.scene.marginRight = insets[1];
        Core.scene.marginTop = insets[2];
        Core.scene.marginBottom = insets[3];

        Tex.load();
        Icon.load();
        Styles.load();
        Tex.loadStyles();
        Fonts.loadContentIcons();

        Dialog.setShowAction(() -> sequence(alpha(0f), fadeIn(0.1f)));
        Dialog.setHideAction(() -> sequence(fadeOut(0.1f)));

        Tooltips.getInstance().animations = false;
        Tooltips.getInstance().textProvider = text -> new Tooltip(t -> t.background(Styles.black6).margin(4f).add(text));

        Core.settings.setErrorHandler(e -> {
            Log.err(e);
            Core.app.post(() -> showErrorMessage("Failed to access local storage.\nSettings will not be saved."));
        });

        ClickListener.clicked = () -> Sounds.press.play();

        Colors.put("accent", Pal.accent);
        Colors.put("unlaunched", Color.valueOf("8982ed"));
        Colors.put("highlight", Pal.accent.cpy().lerp(Color.white, 0.3f));
        Colors.put("stat", Pal.stat);
        Colors.put("negstat", Pal.negativeStat);

        drillCursor = Core.graphics.newCursor("drill", Fonts.cursorScale());
        unloadCursor = Core.graphics.newCursor("unload", Fonts.cursorScale());
        targetCursor = Core.graphics.newCursor("target", Fonts.cursorScale());
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(){
        return Seq.with(new AssetDescriptor<>(Control.class), new AssetDescriptor<>("outline", Font.class), new AssetDescriptor<>("default", Font.class));
    }

    @Override
    public void update(){
        if(disableUI || Core.scene == null) return;

        Events.fire(Trigger.uiDrawBegin);

        Core.scene.act();
        Core.scene.draw();

        if(Core.input.keyTap(KeyCode.mouseLeft) && Core.scene.hasField()){
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(!(e instanceof TextField)){
                Core.scene.setKeyboardFocus(null);
            }
        }

        Events.fire(Trigger.uiDrawEnd);
    }

    @Override
    public void init(){
        billions = Core.bundle.get("unit.billions");
        millions = Core.bundle.get("unit.millions");
        thousands = Core.bundle.get("unit.thousands");

        menuGroup = new WidgetGroup();
        hudGroup = new WidgetGroup();

        menufrag = new MenuFragment();
        hudfrag = new HudFragment();
        hints = new HintsFragment();
        chatfrag = new ChatFragment();
        minimapfrag = new MinimapFragment();
        listfrag = new PlayerListFragment();
        loadfrag = new LoadingFragment();
        consolefrag = new ConsoleFragment();

        picker = new ColorPicker();
        editor = new MapEditorDialog();
        controls = new KeybindDialog();
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
        planet = new PlanetDialog();
        research = new ResearchDialog();
        mods = new ModsDialog();
        schematics = new SchematicsDialog();
        logic = new LogicDialog();
        fullText = new FullTextDialog();
        campaignComplete = new CampaignCompleteDialog();

        Group group = Core.scene.root;

        menuGroup.setFillParent(true);
        menuGroup.touchable = Touchable.childrenOnly;
        menuGroup.visible(() -> state.isMenu());
        hudGroup.setFillParent(true);
        hudGroup.touchable = Touchable.childrenOnly;
        hudGroup.visible(() -> state.isGame());

        Core.scene.add(menuGroup);
        Core.scene.add(hudGroup);

        hudfrag.build(hudGroup);
        menufrag.build(menuGroup);
        chatfrag.build(hudGroup);
        minimapfrag.build(hudGroup);
        listfrag.build(hudGroup);
        consolefrag.build(hudGroup);
        loadfrag.build(group);
        new FadeInFragment().build(group);
    }

    @Override
    public void resize(int width, int height){
        if(Core.scene == null) return;

        int[] insets = Core.graphics.getSafeInsets();
        Core.scene.marginLeft = insets[0];
        Core.scene.marginRight = insets[1];
        Core.scene.marginTop = insets[2];
        Core.scene.marginBottom = insets[3];

        Core.scene.resize(width, height);
        Events.fire(new ResizeEvent());
    }

    public TextureRegionDrawable getIcon(String name){
        if(Icon.icons.containsKey(name)) return Icon.icons.get(name);
        return Core.atlas.getDrawable("error");
    }

    public TextureRegionDrawable getIcon(String name, String def){
        if(Icon.icons.containsKey(name)) return Icon.icons.get(name);
        return getIcon(def);
    }

    public void loadAnd(Runnable call){
        loadAnd("@loading", call);
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
                this.title = (titleText.startsWith("@") ? Core.bundle.get(titleText.substring(1)) : titleText);
                this.text = def;
                this.numeric = inumeric;
                this.maxLength = textLength;
                this.accepted = confirmed;
                this.allowEmpty = false;
            }});
        }else{
            new Dialog(titleText){{
                cont.margin(30).add(dtext).padRight(6f);
                TextFieldFilter filter = inumeric ? TextFieldFilter.digitsOnly : (f, c) -> true;
                TextField field = cont.field(def, t -> {}).size(330f, 50f).get();
                field.setFilter((f, c) -> field.getText().length() < textLength && filter.acceptChar(f, c));
                buttons.defaults().size(120, 54).pad(4);
                buttons.button("@cancel", this::hide);
                buttons.button("@ok", () -> {
                    confirmed.get(field.getText());
                    hide();
                }).disabled(b -> field.getText().isEmpty());
                keyDown(KeyCode.enter, () -> {
                    String text = field.getText();
                    if(!text.isEmpty()){
                        confirmed.get(text);
                        hide();
                    }
                });
                keyDown(KeyCode.escape, this::hide);
                keyDown(KeyCode.back, this::hide);
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
        showInfoFade(info,  7f);
    }

    public void showInfoFade(String info, float duration){
        var cinfo = Core.scene.find("coreinfo");
        Table table = new Table();
        table.touchable = Touchable.disabled;
        table.setFillParent(true);
        if(cinfo.visible && !state.isMenu()) table.marginTop(cinfo.getPrefHeight() / Scl.scl() / 2);
        table.actions(Actions.fadeOut(duration, Interp.fade), Actions.remove());
        table.top().add(info).style(Styles.outlineLabel).padTop(10);
        Core.scene.add(table);
    }

    /** Shows a fading label at the top of the screen. */
    public void showInfoToast(String info, float duration){
        var cinfo = Core.scene.find("coreinfo");
        Table table = new Table();
        table.touchable = Touchable.disabled;
        table.setFillParent(true);
        if(cinfo.visible && !state.isMenu()) table.marginTop(cinfo.getPrefHeight() / Scl.scl() / 2);
        table.update(() -> {
            if(state.isMenu()) table.remove();
        });
        table.actions(Actions.delay(duration * 0.9f), Actions.fadeOut(duration * 0.1f, Interp.fade), Actions.remove());
        table.top().table(Styles.black3, t -> t.margin(4).add(info).style(Styles.outlineLabel)).padTop(10);
        Core.scene.add(table);
        lastAnnouncement = table;
    }

    /** Shows a label at some position on the screen. Does not fade. */
    public void showInfoPopup(String info, float duration, int align, int top, int left, int bottom, int right){
        Table table = new Table();
        table.setFillParent(true);
        table.touchable = Touchable.disabled;
        table.update(() -> {
            if(state.isMenu()) table.remove();
        });
        table.actions(Actions.delay(duration), Actions.remove());
        table.align(align).table(Styles.black3, t -> t.margin(4).add(info).style(Styles.outlineLabel)).pad(top, left, bottom, right);
        Core.scene.add(table);
    }

    /** Shows a label in the world. This label is behind everything. Does not fade. */
    public void showLabel(String info, float duration, float worldx, float worldy){
        var table = new Table(Styles.black3).margin(4);
        table.touchable = Touchable.disabled;
        table.update(() -> {
            if(state.isMenu()) table.remove();
            Vec2 v = Core.camera.project(worldx, worldy);
            table.setPosition(v.x, v.y, Align.center);
        });
        table.actions(Actions.delay(duration), Actions.remove());
        table.add(info).style(Styles.outlineLabel);
        table.pack();
        table.act(0f);
        //make sure it's at the back
        Core.scene.root.addChildAt(0, table);

        table.getChildren().first().act(0f);
    }

    public void showInfo(String info){
        new Dialog(""){{
            getCell(cont).growX();
            cont.margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            keyDown(KeyCode.enter, this::hide);
            closeOnBack();
        }}.show();
    }

    public void showInfoOnHidden(String info, Runnable listener){
        new Dialog(""){{
            getCell(cont).growX();
            cont.margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            hidden(listener);
            closeOnBack();
        }}.show();
    }

    public void showStartupInfo(String info){
        new Dialog(""){{
            getCell(cont).growX();
            cont.margin(15).add(info).width(400f).wrap().get().setAlignment(Align.left);
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            closeOnBack();
        }}.show();
    }

    public void showErrorMessage(String text){
        new Dialog(""){{
            setFillParent(true);
            cont.margin(15f);
            cont.add("@error.title");
            cont.row();
            cont.image().width(300f).pad(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add(text).pad(2f).growX().wrap().get().setAlignment(Align.center);
            cont.row();
            cont.button("@ok", this::hide).size(120, 50).pad(4);
            closeOnBack();
        }}.show();
    }

    public void showException(Throwable t){
        showException("", t);
    }

    public void showException(String text, Throwable exc){
        if(loadfrag == null) return;

        loadfrag.hide();
        new Dialog(""){{
            String message = Strings.getFinalMessage(exc);

            setFillParent(true);
            cont.margin(15);
            cont.add("@error.title").colspan(2);
            cont.row();
            cont.image().width(300f).pad(2).colspan(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add((text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text) + (message == null ? "" : "\n[lightgray](" + message + ")")).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
            cont.row();

            Collapser col = new Collapser(base -> base.pane(t -> t.margin(14f).add(Strings.neatError(exc)).color(Color.lightGray).left()), true);

            cont.button("@details", Styles.togglet, col::toggle).size(180f, 50f).checked(b -> !col.isCollapsed()).fillX().right();
            cont.button("@ok", this::hide).size(110, 50).fillX().left();
            cont.row();
            cont.add(col).colspan(2).pad(2);
            closeOnBack();
        }}.show();
    }

    public void showText(String titleText, String text){
        showText(titleText, text, Align.center);
    }

    public void showText(String titleText, String text, int align){
        new Dialog(titleText){{
            cont.row();
            cont.image().width(400f).pad(2).colspan(2).height(4f).color(Pal.accent);
            cont.row();
            cont.add(text).width(400f).wrap().get().setAlignment(align, align);
            cont.row();
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            closeOnBack();
        }}.show();
    }

    public void showInfoText(String titleText, String text){
        new Dialog(titleText){{
            cont.margin(15).add(text).width(400f).wrap().left().get().setAlignment(Align.left, Align.left);
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            closeOnBack();
        }}.show();
    }

    public void showSmall(String titleText, String text){
        new Dialog(titleText){{
            cont.margin(10).add(text);
            titleTable.row();
            titleTable.image().color(Pal.accent).height(3f).growX().pad(2f);
            buttons.button("@ok", this::hide).size(110, 50).pad(4);
            closeOnBack();
        }}.show();
    }

    public void showConfirm(String text, Runnable confirmed){
        showConfirm("@confirm", text, null, confirmed);
    }

    public void showConfirm(String title, String text, Runnable confirmed){
        showConfirm(title, text, null, confirmed);
    }

    public void showConfirm(String title, String text, Boolp hide, Runnable confirmed){
        BaseDialog dialog = new BaseDialog(title);
        dialog.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.button("@cancel", Icon.cancel, dialog::hide);
        dialog.buttons.button("@ok", Icon.ok, () -> {
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
        dialog.keyDown(KeyCode.enter, () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(KeyCode.escape, dialog::hide);
        dialog.keyDown(KeyCode.back, dialog::hide);
        dialog.show();
    }

    public void showCustomConfirm(String title, String text, String yes, String no, Runnable confirmed, Runnable denied){
        BaseDialog dialog = new BaseDialog(title);
        dialog.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.button(no, () -> {
            dialog.hide();
            denied.run();
        });
        dialog.buttons.button(yes, () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(KeyCode.escape, dialog::hide);
        dialog.keyDown(KeyCode.back, dialog::hide);
        dialog.show();
    }

    public boolean hasAnnouncement(){
        return lastAnnouncement != null && lastAnnouncement.parent != null;
    }

    /** Display text in the middle of the screen, then fade out. */
    public void announce(String text){
        announce(text, 3);
    }

    /** Display text in the middle of the screen, then fade out. */
    public void announce(String text, float duration){
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(8f).add(text).style(Styles.outlineLabel).labelAlign(Align.center);
        t.update(() -> t.setPosition(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f, Align.center));
        t.actions(Actions.fadeOut(duration, Interp.pow4In), Actions.remove());
        t.pack();
        t.act(0.1f);
        Core.scene.add(t);
        lastAnnouncement = t;
    }

    public void showOkText(String title, String text, Runnable confirmed){
        BaseDialog dialog = new BaseDialog(title);
        dialog.cont.add(text).width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.button("@ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.show();
    }

    /** Shows a menu that fires a callback when an option is selected. If nothing is selected, -1 is returned. */
    public void showMenu(String title, String message, String[][] options, Intc callback){
        new Dialog("[accent]" + title){{
            setFillParent(true);
            removeChild(titleTable);
            cont.add(titleTable).width(400f);

            getStyle().titleFontColor = Color.white;
            title.getStyle().fontColor = Color.white;
            title.setStyle(title.getStyle());

            cont.row();
            cont.image().width(400f).pad(2).colspan(2).height(4f).color(Pal.accent).bottom();
            cont.row();
            cont.pane(table -> {
                table.add(message).width(400f).wrap().get().setAlignment(Align.center);
                table.row();

                int option = 0;
                for(var optionsRow : options){
                    Table buttonRow = table.row().table().get().row();
                    int fullWidth = 400 - (optionsRow.length - 1) * 8; // adjust to count padding as well
                    int width = fullWidth / optionsRow.length;
                    int lastWidth = fullWidth - width * (optionsRow.length - 1); // take the rest of space for uneven table

                    for(int i = 0; i < optionsRow.length; i++){
                        if(optionsRow[i] == null) continue;

                        String optionName = optionsRow[i];
                        int finalOption = option;
                        buttonRow.button(optionName, () -> {
                            callback.get(finalOption);
                            hide();
                        }).size(i == optionsRow.length - 1 ? lastWidth : width, 50).pad(4);
                        option++;
                    }
                }
            }).growX();
            closeOnBack(() -> callback.get(-1));
        }}.show();
    }

    /** Formats time with hours:minutes:seconds. */
    public static String formatTime(float ticks){
        int seconds = (int)(ticks / 60);
        if(seconds < 60) return "0:" + (seconds < 10 ? "0" : "") + seconds;

        int minutes = seconds / 60;
        int modSec = seconds % 60;
        if(minutes < 60) return minutes + ":" + (modSec < 10 ? "0" : "") + modSec;

        int hours = minutes / 60;
        int modMinute = minutes % 60;

        return hours + ":" + (modMinute < 10 ? "0" : "") + modMinute + ":" + (modSec < 10 ? "0" : "") + modSec;
    }

    public static String formatAmount(long number){
        //prevent things like bars displaying erroneous representations of casted infinities
        if(number == Long.MAX_VALUE) return "∞";
        if(number == Long.MIN_VALUE) return "-∞";

        long mag = Math.abs(number);
        String sign = number < 0 ? "-" : "";
        if(mag >= 1_000_000_000){
            return sign + Strings.fixed(mag / 1_000_000_000f, 1) + "[gray]" + billions + "[]";
        }else if(mag >= 1_000_000){
            return sign + Strings.fixed(mag / 1_000_000f, 1) + "[gray]" + millions + "[]";
        }else if(mag >= 10_000){
            return number / 1000 + "[gray]" + thousands + "[]";
        }else if(mag >= 1000){
            return sign + Strings.fixed(mag / 1000f, 1) + "[gray]" + thousands + "[]";
        }else{
            return number + "";
        }
    }

    public static int roundAmount(int number){
        if(number >= 1_000_000_000){
            return Mathf.round(number, 100_000_000);
        }else if(number >= 1_000_000){
            return Mathf.round(number, 100_000);
        }else if(number >= 10_000){
            return Mathf.round(number, 1000);
        }else if(number >= 1000){
            return Mathf.round(number, 100);
        }else if(number >= 100){
            return Mathf.round(number, 100);
        }else if(number >= 10){
            return Mathf.round(number, 10);
        }else{
            return number;
        }
    }
}
