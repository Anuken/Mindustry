package mindustry.ui;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** Class for handling menus and notifications across the network. Unstable API! */
public class Menus{
    private static IntMap<MenuListener> menuListeners = new IntMap<>();

    /** Register a *global* menu listener. If no option is chosen, the option is returned as -1. */
    public static void registerMenu(int id, MenuListener listener){
        menuListeners.put(id, listener);
    }

    //do not invoke any of the methods below directly, use Call

    @Remote(variants = Variant.both)
    public static void menu(int menuId, String title, String message, String[][] options){
        if(title == null) title = "";
        if(options == null) options = new String[0][0];

        ui.showMenu(title, message, options, (option) -> Call.menuChoose(player, menuId, option));
    }

    @Remote(targets = Loc.both, called = Loc.both)
    public static void menuChoose(@Nullable Player player, int menuId, int option){
        if(player != null && menuListeners.containsKey(menuId)){
            Events.fire(new MenuOptionChooseEvent(player, menuId, option));
            menuListeners.get(menuId).get(player, option);
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

    @Remote(variants = Variant.both)
    public static void infoPopup(String message, float duration, int align, int top, int left, int bottom, int right){
        if(message == null) return;

        ui.showInfoPopup(message, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both)
    public static void label(String message, float duration, float worldx, float worldy){
        if(message == null) return;

        ui.showLabel(message, duration, worldx, worldy);
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

    public interface MenuListener{
        void get(Player player, int option);
    }
}
