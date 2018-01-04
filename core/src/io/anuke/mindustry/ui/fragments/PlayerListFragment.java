package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class PlayerListFragment implements Fragment{
    Table content = new Table();

    @Override
    public void build(){
        new table(){{
            new table("pane"){{
                new label(() -> Bundles.format("text.players", Vars.control.playerGroup.amount()));
                row();
                add(content).grow();
            }}.end();

            visible(() -> Inputs.keyDown("player_list"));
        }}.end();

        rebuild();
    }

    public void rebuild(){
        if(!Net.active()) return;
        content.clear();

        float h = 80f;

        for(Player player : Vars.control.playerGroup.all()){
            Table button = new Table("button");
            button.left();
            button.margin(0);
            BorderImage image = new BorderImage(Draw.region(player.isAndroid ? "ship-standard" : "mech-standard"), 3f);
            button.add(image).size(h);
            button.add(player.name).pad(10);

            if(Net.server() && !player.isLocal){
                button.add().growY();
                button.addIButton("icon-cancel", 14*3, () ->
                    Net.kickConnection(player.clientid)
                ).size(h);
            }

            content.add(button).padBottom(-5).width(250f);
            content.row();
        }

        content.marginBottom(5);
    }

}
