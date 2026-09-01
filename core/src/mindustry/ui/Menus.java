package mindustry.ui;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.builder.*;
import mindustry.ui.builder.UiBuilder.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

/** Class for handling menus and notifications across the network. */
public class Menus{
    private static IntMap<Dialog> followUpMenus = new IntMap<>();
    private static IntMap<MenuDialog> menuDialogs = new IntMap<>();

    private static final Seq<MenuListener> menuListeners = new Seq<>();
    private static final Seq<MenuBuilderListener> menuBuilderListeners = new Seq<>();
    private static final Seq<TextInputListener> textInputListeners = new Seq<>();

    /** Register a global menu listener. If no option is chosen, the option is returned as -1. This API is soft-deprecated; use {@link MenuBuilder} and {@link #registerMenuBuilder} instead. */
    public static int registerMenu(MenuListener listener){
        menuListeners.add(listener);
        return menuListeners.size - 1;
    }

    /** Register a global menu builder listener. This is not the same as the 'old' MenuListener API; use it with {@link MenuBuilder}. */
    public static int registerMenuBuilder(MenuBuilderListener listener){
        menuBuilderListeners.add(listener);
        return menuBuilderListeners.size - 1;
    }

    /** Register a *global* text input listener. If no text is provided, the text is returned as null. */
    public static int registerTextInput(TextInputListener listener){
        textInputListeners.add(listener);
        return textInputListeners.size - 1;
    }

    //use the MenuBuilder version instead of invoking this with Call
    @Remote(variants = Variant.both)
    public static void menuBuilder(int id, long token, @Nullable String title, boolean hideOnClick, boolean hidePrevious, boolean fillScreen, NodeBuilder<?> builder){
        MenuDialog dialog = new MenuDialog();
        dialog.setFillParent(fillScreen);
        if(title != null){
            dialog.title.setText(title);
        }else{
            dialog.titleTable.remove();
        }

        dialog.addCloseButton();

        dialog.update(() -> {
            if(!net.active()){
                dialog.hide();
            }
        });

        dialog.hidden(() -> {
            menuDialogs.remove(id);
            //let the server know nothing was selected
            if(!dialog.wasHidden){
                var result = new MenuResult();
                result.token = token;
                if(net.client()) Call.menuBuilderChoose(player, id, result);
            }
        });

        dialog.originalListener = result -> {
            dialog.wasHidden = true; //don't trigger the hidden listener
            result.token = token;
            if(net.client()) Call.menuBuilderChoose(player, id, result);

            if(hideOnClick){
                dialog.hide();
            }
        };

        var result = UiTreeBuilder.build(dialog.cont, builder, dialog.originalListener);
        dialog.idToElement = result.idElements;
        dialog.addImages(result.images);

        var oldDialog = menuDialogs.remove(id);

        if(hidePrevious && oldDialog != null){
            //old dialog exists, replace it (show with no action)
            dialog.show(Core.scene, null);
            oldDialog.hide(null);
        }else{
            dialog.show();
        }

        menuDialogs.put(id, dialog);
    }

    @Remote(variants = Variant.both)
    public static void menuBuilderUpdate(int id, String tableId, NodeBuilder<?> builder){
        MenuDialog dialog = menuDialogs.get(id);
        if(dialog == null) return;
        Element element = dialog.idToElement.get(tableId);
        Table table = element instanceof Table t ? t : null;
        if(table == null && element instanceof ScrollPane pane && pane.getChildren().size > 0 && pane.getChildren().first() instanceof Table t) table = t;
        if(table == null) return;

        table.clear();
        var result = UiTreeBuilder.build(table, builder, res -> {
            //forward results to original listener
            dialog.originalListener.get(res);
        });
        //save new element IDs and images
        dialog.idToElement.putAll(result.idElements);
        dialog.addImages(result.images);
    }

    @Remote(variants = Variant.both)
    public static void hideMenuBuilder(int menuId) {
        if(!menuDialogs.containsKey(menuId)) return;
        menuDialogs.remove(menuId).hide();
    }

    //do not invoke any of the methods below directly, use Call

    @Remote(targets = Loc.both, called = Loc.both)
    public static void menuBuilderChoose(@Nullable Player player, int menuId, MenuResult results){
        if(player != null){
            Events.fire(new MenuBuilderOptionChooseEvent(player, menuId, results));

            if(menuId >= 0 && menuId < menuBuilderListeners.size){
                menuBuilderListeners.get(menuId).get(player, results);
            }
        }
    }

    @Remote(variants = Variant.both)
    public static void menu(int menuId, String title, String message, String[][] options){
        if(title == null) title = "";
        if(message == null) message = "";
        if(options == null) options = new String[0][0];

        showMenu(title, message, options, (option) -> Call.menuChoose(player, menuId, option));
    }

    @Remote(variants = Variant.both)
    public static void followUpMenu(int menuId, String title, String message, String[][] options){
        if(title == null) title = "";
        if(message == null) message = "";
        if(options == null) options = new String[0][0];

        showFollowUpMenu(menuId, title, message, options, (option) -> Call.menuChoose(player, menuId, option));
    }

    @Remote(variants = Variant.both)
    public static void hideFollowUpMenu(int menuId) {
        if(!followUpMenus.containsKey(menuId)) return;
        followUpMenus.remove(menuId).hide();
    }

    static Dialog newMenuDialog(String title, String message, String[][] options, Cons2<Integer, Dialog> buttonListener){
        return new Dialog(title){{
            setFillParent(true);
            removeChild(titleTable);
            cont.add(titleTable).width(400f);

            cont.row();
            cont.image().width(400f).pad(2).colspan(2).height(4f).color(Pal.accent).bottom();
            cont.row();
            cont.pane(table -> {
                table.add(message).width(400f).wrap().get().setAlignment(Align.center);
                table.row();

                int option = 0;
                for(var optionsRow : options){
                    if(optionsRow.length == 0) continue;
                    Table buttonRow = table.row().table().get().row();
                    int fullWidth = 400 - (optionsRow.length - 1) * 8; // adjust to count padding as well
                    int width = fullWidth / optionsRow.length;
                    int lastWidth = fullWidth - width * (optionsRow.length - 1); // take the rest of space for uneven table

                    for(int i = 0; i < optionsRow.length; i++){
                        if(optionsRow[i] == null) continue;

                        String optionName = optionsRow[i];
                        int finalOption = option;
                        buttonRow.button(optionName, () -> buttonListener.get(finalOption, this))
                        .size(i == optionsRow.length - 1 ? lastWidth : width, 50).pad(4);
                        option++;
                    }
                }
            }).growX();
        }};
    }

    /** Shows a menu that fires a callback when an option is selected. If nothing is selected, -1 is returned. */
    static void showMenu(String title, String message, String[][] options, Intc callback){
        Dialog dialog = newMenuDialog(title, message, options, (option, myself) -> {
            callback.get(option);
            myself.hide();
        });
        dialog.closeOnBack(() -> callback.get(-1));
        dialog.show();
    }

    /** Shows a menu that hides when another followUp-menu is shown or when nothing is selected.
     * @see #showMenu(String, String, String[][], Intc) */
    static void showFollowUpMenu(int menuId, String title, String message, String[][] options, Intc callback) {
        Dialog dialog = newMenuDialog(title, message, options, (option, myself) -> {
            callback.get(option);
            if(!state.isGame()){
                myself.hide();
            }
        });
        dialog.closeOnBack(() -> {
            followUpMenus.remove(menuId);
            callback.get(-1);
        });

        Dialog oldDialog = followUpMenus.remove(menuId);
        if(oldDialog != null){
            dialog.show(Core.scene, null);
            oldDialog.hide(null);
        }else{
            dialog.show();
        }
        followUpMenus.put(menuId, dialog);
    }

    @Remote(targets = Loc.both, called = Loc.both)
    public static void menuChoose(@Nullable Player player, int menuId, int option){
        if(player != null){
            Events.fire(new MenuOptionChooseEvent(player, menuId, option));
            if(menuId >= 0 && menuId < menuListeners.size){
                menuListeners.get(menuId).get(player, option);
            }
        }
    }

    @Remote(variants = Variant.both)
    public static void textInput(int textInputId, String title, String message, int textLength, String def, boolean numeric){
        textInput(textInputId, title, message, textLength, def, numeric, false);
    }

    @Remote(variants = Variant.both)
    public static void textInput(int textInputId, String title, String message, int textLength, String def, boolean numeric, boolean allowEmpty){
        if(title == null) title = "";
        if(message == null) message = "";
        if(def == null) def = "";

        ui.showTextInput(title, message, textLength, def, numeric, allowEmpty, (text) -> {
            Call.textInputResult(player, textInputId, text);
        }, () -> {
            Call.textInputResult(player, textInputId, null);
        });
    }

    @Remote(targets = Loc.both, called = Loc.both)
    public static void textInputResult(@Nullable Player player, int textInputId, @Nullable String text){
        if(player != null){
            Events.fire(new TextInputEvent(player, textInputId, text));
            if(textInputId >= 0 && textInputId < textInputListeners.size){
                textInputListeners.get(textInputId).get(player, text);
            }
        }
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void setHudText(String message){
        if(message == null) return;

        ui.hudfrag.setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void hideHudText(){
        ui.hudfrag.toggleHudText(false);
    }

    /** TCP version */
    @Remote(variants = Variant.both)
    public static void setHudTextReliable(String message){
        setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void announce(String message){
        if(message == null) return;

        ui.announce(message);
    }

    @Remote(variants = Variant.both)
    public static void infoMessage(String message){
        if(message == null) return;

        ui.showText("", message);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void infoPopup(@Nullable String message, @Nullable String id, float duration, int align, int top, int left, int bottom, int right){
        ui.showInfoPopup(message, id, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both)
    public static void infoPopupReliable(@Nullable String message, @Nullable String id, float duration, int align, int top, int left, int bottom, int right){
        infoPopup(message, id, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void infoPopup(@Nullable String message, float duration, int align, int top, int left, int bottom, int right){
        infoPopup(message, null, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both)
    public static void infoPopupReliable(@Nullable String message, float duration, int align, int top, int left, int bottom, int right){
        infoPopup(message, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void label(@Nullable String message, int id, float duration, float worldx, float worldy, int flags){
        ui.showLabel(message, id, duration, worldx, worldy, flags);
    }

    @Remote(variants = Variant.both)
    public static void labelReliable(@Nullable String message, int id, float duration, float worldx, float worldy, int flags){
        label(message, id, duration, worldx, worldy, flags);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void label(@Nullable String message, int id, float duration, float worldx, float worldy){
        label(message, id, duration, worldx, worldy, WorldLabel.flagBackground | WorldLabel.flagAutoscale | WorldLabel.flagOutline);
    }

    @Remote(variants = Variant.both)
    public static void labelReliable(@Nullable String message, int id, float duration, float worldx, float worldy){
        label(message, id, duration, worldx, worldy);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void label(@Nullable String message, float duration, float worldx, float worldy){
        label(message, -1, duration, worldx, worldy);
    }

    @Remote(variants = Variant.both)
    public static void labelReliable(@Nullable String message, float duration, float worldx, float worldy){
        label(message, -1, duration, worldx, worldy);
    }

    @Remote(variants = Variant.both)
    public static void infoToast(String message, float duration){
        if(message == null) return;

        ui.showInfoToast(message, duration);
    }

    @Remote(variants = Variant.both)
    public static void warningToast(int unicode, String text){
        if(text == null || Fonts.icon.getData().getGlyph((char)unicode) == null) return;

        ui.hudfrag.showToast(Fonts.getGlyph(Fonts.icon, (char)unicode), text);
    }

    @Remote(variants = Variant.both)
    public static void openURI(String uri){
        if(uri == null) return;

        ui.showConfirm(Core.bundle.format("linkopen", uri), () -> Core.app.openURI(uri));
    }

    @Remote(variants = Variant.both)
    public static void copyToClipboard(String text){
        if(text == null) return;

        ui.showConfirm(Core.bundle.format("clipboardcopy", text), () -> Core.app.setClipboardText(text));
    }

    //internal use only
    @Remote(variants = Variant.both)
    public static void removeWorldLabel(int id){
        var label = Groups.sync.getByID(id);
        if(label instanceof WorldLabelc){
            label.remove();
        }
    }

    static class MenuDialog extends BaseDialog{
        //retained for easy lookup if an element needs to be replaced
        ObjectMap<String, Element> idToElement;
        //every streamed image built into this dialog, keyed by the region name
        ObjectMap<String, Seq<Image>> images = new ObjectMap<>();
        Cons<MenuResult> originalListener;
        boolean wasHidden;

        static{
            // refresh images that are received after the dialog is already opened
            Events.on(TextureStreamEvent.class, e -> {
                for(var dialog : menuDialogs.values()){
                    var imgs = dialog.images.get(e.name);
                    if(imgs != null){
                        var drawable = Core.atlas.drawable(e.name);
                        imgs.each(img -> img.setDrawable(drawable));
                    }
                }
            });
        }

        public MenuDialog(){
            super("");
        }

        void addImages(ObjectMap<String, Seq<Image>> images){
            images.each((region, imgs) -> this.images.get(region, Seq::new).addAll(imgs));
        }
    }

    public interface MenuListener{
        void get(Player player, int option);
    }

    public interface MenuBuilderListener{
        void get(Player player, MenuResult result);
    }

    public interface TextInputListener{
        void get(Player player, @Nullable String text);
    }
}
