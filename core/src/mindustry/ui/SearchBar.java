package mindustry.ui;

import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;

public class SearchBar{

    public static <T> Table add(Table parent, Seq<T> list, Func<String, String> queryf,
            Func<T, String> namef, Cons2<Table, T> itemc, boolean show){
        Table[] pane = {null};

        Cons<String> rebuild = str -> {
            String query = queryf.get(str);

            pane[0].clear();
            boolean any = false;
            for(T item : list){
                if(query.isEmpty() || matches(query, namef.get(item))){
                    any = true;
                    itemc.get(pane[0], item);
                }
            }

            if(!any){
                pane[0].add("@none.found").color(Color.lightGray).pad(4);
            }
        };

        if(show){
            parent.table(search -> {
                search.image(Icon.zoom).padRight(8f);
                search.field("", rebuild).growX();
            }).fillX().padBottom(4);
        }

        parent.row();
        parent.pane(table -> {
            pane[0] = table;
            rebuild.get("");
        }).get().setScrollingDisabled(true, false);
        return pane[0];
    }

    public static <T> Table add(Table parent, Seq<T> list, Func<String, String> queryf, Func<T, String> namef, Cons2<Table, T> itemc){
        return add(parent, list, queryf, namef, itemc, true);
    }

    public static <T> Table add(Table parent, Seq<T> list, Func<T, String> namef, Cons2<Table, T> itemc, boolean show){
        return add(parent, list, String::toLowerCase, namef, itemc, show);
    }

    /** Match a list item with the search query, case insensitive */
    public static boolean matches(String query, String name){
        return name != null && !name.isEmpty() && name.toLowerCase().contains(query);
    }
}
