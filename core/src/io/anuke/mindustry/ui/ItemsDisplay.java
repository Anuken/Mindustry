package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.*;

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

        table(Tex.button,t -> {
            t.margin(10).marginLeft(15).marginTop(15f);
            t.label(() -> state.is(State.menu) ? "$launcheditems" : "$launchinfo").colspan(3).padBottom(4).left().colspan(3).width(210f).wrap();
            t.row();
            for(Item item : content.items()){
                if(item.type == ItemType.material && data.isUnlocked(item)){
                    t.label(() -> format(item)).left();
                    t.addImage(item.icon(Cicon.small)).size(8 * 3).padLeft(4).padRight(4);
                    t.add(item.localizedName()).color(Color.lightGray).left();
                    t.row();
                }
            }
        });
    }

    private String format(Item item){
        builder.setLength(0);
        builder.append(ui.formatAmount(data.items().get(item, 0)));
        if(!state.is(State.menu) && !state.teams.get(player.getTeam()).cores.isEmpty() && state.teams.get(player.getTeam()).cores.first().entity != null && state.teams.get(player.getTeam()).cores.first().entity.items.get(item) > 0){
            builder.append(" [unlaunched]+ ");
            builder.append(ui.formatAmount(state.teams.get(player.getTeam()).cores.first().entity.items.get(item)));
        }
        return builder.toString();
    }
}
