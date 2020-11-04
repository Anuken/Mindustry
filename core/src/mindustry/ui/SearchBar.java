package mindustry.ui;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;

public class SearchBar{
    public static <T> Table add(Table parent, Seq<T> list, Func<String, String> queryf,
            Func<T, String> namef, Cons2<Table, T> itemf){
        Cons<String>[] rebuild = new Cons[] {null};
        Table[] pane = new Table[] {null};

        parent.table(search -> {
            rebuild[0] = str -> {
                final String query = queryf.get(str);

                pane[0].clear();
                list.each(item -> {
                    if(query.isEmpty() || matches(query, namef.get(item))){
                        itemf.get(pane[0], item);
                    }
                });
            };

            search.image(Icon.zoom).padRight(8f);
            search.field("", rebuild[0]).growX();
        }).fillX().padBottom(4);

        parent.row();
        parent.pane(table -> {
            pane[0] = table;
            rebuild[0].get("");
        });
        return pane[0];
    }

    public static <T> Table add(Table parent, Seq<T> list, Func<T, String> namef, Cons2<Table, T> itemf){
        return add(parent, list, String::toLowerCase, namef, itemf);
    }

    /** Match a list item with the search query, case insensitive */
    public static boolean matches(String query, String name){
        if(name == null || name.isEmpty()){
            return false;
        }
        return name.toLowerCase().contains(query);
    }
}
