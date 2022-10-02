package mindustry.ui;

import arc.graphics.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Displays a list of items, e.g. launched items.*/
public class ItemsDisplay extends Table{
    boolean collapsed;

    public ItemsDisplay(){
        rebuild(new ItemSeq());
    }

    public void rebuild(ItemSeq items){
        rebuild(items, null);
    }

    public void rebuild(ItemSeq items, @Nullable boolean[] shine){
        clear();
        top().left();
        margin(0);

        table(Tex.button, c -> {
            c.margin(10).marginLeft(12).marginTop(15f);
            c.marginRight(12f);
            c.left();

            Collapser col = new Collapser(base -> base.pane(t -> {
                t.marginRight(30f);
                t.left();
                for(Item item : content.items()){
                    if(!items.has(item)) continue;

                    Label label = t.add(UI.formatAmount(items.get(item))).left().get();
                    t.image(item.uiIcon).size(8 * 3).padLeft(4).padRight(4);
                    t.add(item.localizedName).color(Color.lightGray).left();
                    t.row();

                    if(shine != null && shine[item.id]){
                        label.setColor(Pal.accent);
                        label.actions(Actions.color(Color.white, 0.75f, Interp.fade));
                    }
                }
            }).scrollX(false), false).setDuration(0.3f);

            col.setCollapsed(collapsed, false);

            c.button("@globalitems", Icon.downOpen, Styles.flatTogglet, col::toggle).update(t -> {
                t.setChecked(col.isCollapsed());
                collapsed = col.isCollapsed();
                ((Image)t.getChildren().get(1)).setDrawable(col.isCollapsed() ? Icon.upOpen : Icon.downOpen);
            }).padBottom(4).left().fillX().margin(12f).minWidth(200f);
            c.row();
            c.add(col);
        });
    }
}
