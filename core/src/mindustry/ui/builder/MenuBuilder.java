package mindustry.ui.builder;

import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.builder.UiBuilder.*;

/** Builder for presenting a complex menu to the player. */
public class MenuBuilder{
    /** Title string; if null, no title is shown at all (the orange bar is removed too) */
    public @Nullable String title;
    /** If false, the dialog will not immediately be hidden when a button with a click listener is clicked. keep in mind that users will be able to spam buttons. */
    public boolean hideOnClick = true;
    /** If true (default), existing menus with this ID are hidden. */
    public boolean hideExisting = true;
    /** If true (default), the dialog fills the screen. If you're showing something small, make it false. For large complex menus, this has to be true in order to work properly. */
    public boolean fillScreen = true;
    /** Unique value that is passed to the result after menu is closed. */
    public long token;
    /** ID for menu callback; you can reuse this as long as you have unique keys for button results. */
    public int id;
    /** contents of the dialog, must not be null */
    public NodeBuilder<?> ui;

    /** @return a menu constructed from a builder. */
    public static MenuBuilder of(NodeBuilder<?> ui){
        var builder = new MenuBuilder();
        builder.ui = ui;
        return builder;
    }

    /** @return a menu constructed from a DSL string (see UI DSL docs). */
    public static MenuBuilder of(String dslString){
        var builder = new MenuBuilder();
        builder.ui = UiDslParser.parse(dslString);
        return builder;
    }

    /** Submits this menu to the relevant player. */
    public void show(Player player){
        Call.menuBuilder(player.con, id, token, title, hideOnClick, hideExisting, fillScreen, ui);
    }

    /** Submits this menu to all players. */
    public void showAll(){
        Call.menuBuilder(id, token, title, hideOnClick, hideExisting, fillScreen, ui);
    }

    /** Rebuilds a specific Table with the specified elementId. This can be used for updating dialog content based on user interaction.*/
    public void update(Player player, String elementId){
        Call.menuBuilderUpdate(player.con, id, elementId, ui);
    }

    public MenuBuilder hideExisting(boolean hideExisting){
        this.hideExisting = hideExisting;
        return this;
    }

    public MenuBuilder token(long token){
        this.token = token;
        return this;
    }

    public MenuBuilder title(String title){
        this.title = title;
        return this;
    }

    public MenuBuilder hideOnClick(boolean hideOnClick){
        this.hideOnClick = hideOnClick;
        return this;
    }

    public MenuBuilder fillScreen(boolean fillScreen){
        this.fillScreen = fillScreen;
        return this;
    }

    public MenuBuilder id(int id){
        this.id = id;
        return this;
    }
}
