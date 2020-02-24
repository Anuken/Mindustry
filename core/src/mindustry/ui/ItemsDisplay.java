package mindustry.ui;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Displays a list of items, e.g. launched items.*/
public class ItemsDisplay extends Table{
    private StringBuilder builder = new StringBuilder();

    public ItemsDisplay(){
        rebuild();
    }

    public void rebuild(){
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
                    if(item.type == ItemType.material && data.isUnlocked(item)){
                        t.label(() -> format(item)).left();
                        t.addImage(item.icon(Cicon.small)).size(8 * 3).padLeft(4).padRight(4);
                        t.add(item.localizedName).color(Color.lightGray).left();
                        t.row();
                    }
                }
            }).get().setScrollingDisabled(true, false), false).setDuration(0.3f);

            c.addImageTextButton("$launcheditems", Icon.downOpen, Styles.clearTogglet, col::toggle).update(t -> {
                t.setText(state.is(State.menu) ? "$launcheditems" : "$launchinfo");
                t.setChecked(col.isCollapsed());
                ((Image)t.getChildren().get(1)).setDrawable(col.isCollapsed() ? Icon.upOpen : Icon.downOpen);
            }).padBottom(4).left().fillX().margin(12f).minWidth(200f);
            c.row();
            c.add(col);
        });
    }

    private String format(Item item){
        builder.setLength(0);
        builder.append(ui.formatAmount(data.items().get(item, 0)));
        if(!state.is(State.menu) && player.getTeam().data().hasCore() && player.getTeam().core().items.get(item) > 0){
            builder.append(" [unlaunched]+ ");
            builder.append(ui.formatAmount(state.teams.get(player.getTeam()).core().items.get(item)));
        }
        return builder.toString();
    }
}
