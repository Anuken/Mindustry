package mindustry.world.blocks;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class ItemSelection{
    private static TextField search;
    private static float scrollPos = 0f;
    private static int rowCount;

    public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer){
        buildTable(table, items, holder, consumer, true);
    }

    public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer, boolean closeSelect){
        buildTable(table, items, holder, consumer, closeSelect, 5, 4);
    }

    public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer, int columns){
        buildTable(table, items, holder, consumer, true, 5, columns);
    }

    public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer, int rows, int columns){
        buildTable(table, items, holder, consumer, true, rows, columns);
    }
    
    public static <T extends UnlockableContent> void buildTable(Table table, Seq<T> items, Prov<T> holder, Cons<T> consumer, boolean closeSelect, int rows, int columns){
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table().top();
        cont.defaults().size(40);

        Runnable[] rebuild = {null};
        if(search != null) search.clearText();

        rebuild[0] = () -> {
            group.clear();
            cont.clearChildren();

            var text = search != null ? search.getText() : "";
            int i = 0;
            rowCount = 0;

            Seq<T> list = items.select(u -> (text.isEmpty() || u.localizedName.toLowerCase().contains(text.toLowerCase())));
            for(T item : list){
                if(!item.unlockedNow()) continue;

                ImageButton button = cont.button(Tex.whiteui, Styles.clearTogglePartiali, Mathf.clamp(item.selectionSize, 0f, 40f), () -> {
                    if(closeSelect) control.input.frag.config.hideConfig();
                }).tooltip(item.localizedName).group(group).get();
                button.changed(() -> consumer.get(button.isChecked() ? item : null));
                button.getStyle().imageUp = new TextureRegionDrawable(item.uiIcon);
                button.update(() -> button.setChecked(holder.get() == item));

                if(i++ % columns == (columns - 1)){
                    cont.row();
                    rowCount++;
                }
            }

            //add extra blank spaces so it looks nice
            if(i % columns != 0){
                int remaining = columns - (i % columns);
                for(int j = 0; j < remaining; j++){
                    cont.image(Styles.none);
                }
            }
        };

        rebuild[0].run();

        Table main = new Table().background(Styles.black6);
        if(rowCount > rows * 1.5f){
            search = main.field(null, text -> rebuild[0].run()).width(40 * columns).padBottom(4).left().growX().get();
            search.setMessageText("@players.search");
            main.row();
        }

        ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        pane.setScrollYForce(scrollPos);
        pane.update(() -> scrollPos = pane.getScrollY());
        main.add(pane).maxHeight(40 * rows);
        table.top().add(main);
    }
}
