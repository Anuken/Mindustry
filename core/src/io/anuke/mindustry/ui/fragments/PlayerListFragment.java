package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class PlayerListFragment implements Fragment{
    public boolean visible = false;
    Table content = new Table();
    int last = 0;

    @Override
    public void build(){
        new table(){{
            new table("pane"){{
                margin(14f);
                new label(() -> Bundles.format(playerGroup.size() == 1 ? "text.players.single" :
                        "text.players", playerGroup.size()));
                row();
                content.marginRight(13f).marginLeft(13f);
                ScrollPane pane = new ScrollPane(content, "clear");
                pane.setScrollingDisabled(true, false);
                pane.setFadeScrollBars(false);
                add(pane).grow();
                row();
                new table("pane"){{
                    margin(12f);
                    get().addCheck("$text.server.friendlyfire", b -> {
                        state.friendlyFire = b;
                        NetEvents.handleFriendlyFireChange(b);
                    }).growX().update(i -> i.setChecked(state.friendlyFire)).disabled(b -> Net.client());
                }}.pad(10f).growX().end();
            }}.end();

            update(t -> {
                if(!android){
                    visible = Inputs.keyDown("player_list");
                }
                if(!(Net.active() && !state.is(State.menu))){
                    visible = false;
                }
                if(playerGroup.size() != last){
                    rebuild();
                    last = playerGroup.size();
                }
            });

            visible(() -> visible);
        }}.end();

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 60f;

        for(Player player : playerGroup.all()){
            Table button = new Table("button");
            button.left();
            button.margin(5).marginBottom(10);
            BorderImage image = new BorderImage(Draw.region(player.isAndroid ? "ship-standard" : "mech-standard"), 3f);
            button.add(image).size(h);
            button.add(player.name).pad(10);
            button.add().grow();

            if(Net.server() && !player.isLocal){
                button.add().growY();
                button.addImageButton("icon-cancel", 14*3, () ->
                    Net.kickConnection(player.clientid, KickReason.kick)
                ).pad(-5).padBottom(-10).size(h+10, h+14);
            }

            content.add(button).padBottom(-5).width(350f);
            content.row();
        }

        content.marginBottom(5);
    }

}
