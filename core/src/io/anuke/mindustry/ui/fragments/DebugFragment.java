package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.Label;

import static io.anuke.mindustry.Vars.*;

public class DebugFragment extends Fragment{

    public static String debugInfo(){
        int totalUnits = 0;
        for(EntityGroup<?> group : unitGroups){
            totalUnits += group.size();
        }

        totalUnits += playerGroup.size();

        StringBuilder result = join(
                "net.active: " + Net.active(),
                "net.server: " + Net.server(),
                "net.client: " + Net.client(),
                "state: " + state.getState(),
                "units: " + totalUnits,
                "bullets: " + bulletGroup.size(),
                Net.client() ?
                        "chat.open: " + ui.chatfrag.chatOpen() + "\n" +
                                "chat.messages: " + ui.chatfrag.getMessagesSize() + "\n" +
                                "client.connecting: " + netClient.isConnecting() + "\n" : "",
                "players: " + playerGroup.size(),
                "tiles: " + tileGroup.size(),
                "tiles.sleeping: " + TileEntity.sleepingEntities,
                "time: " + Timers.time(),
                "state.gameover: " + state.gameOver,
                "state: " + state.getState()
        );

        result.append("players: ");

        for(Player player : playerGroup.all()){
            result.append("   name: ");
            result.append(player.name);
            result.append("\n");
            result.append("   id: ");
            result.append(player.id);
            result.append("\n");
            result.append("   cid: ");
            result.append(player.con == null ? -1 : player.con.id);
            result.append("\n");
            result.append("   dead: ");
            result.append(player.isDead());
            result.append("\n");
            result.append("   pos: ");
            result.append(player.x);
            result.append(", ");
            result.append(player.y);
            result.append("\n");
            result.append("   mech: ");
            result.append(player.mech);
            result.append("\n");
            result.append("   local: ");
            result.append(player.isLocal);
            result.append("\n");

            result.append("\n");
        }

        return result.toString();
    }

    private static StringBuilder join(String... strings){
        StringBuilder builder = new StringBuilder();
        for(String string : strings){
            builder.append(string);
            builder.append("\n");
        }
        return builder;
    }

    @Override
    public void build(Group parent){
        parent.fill(t -> {
            t.top().left().visible(() -> console);

            t.table("pane", p -> {
                p.defaults().fillX();

                p.pane("clear", new Label(DebugFragment::debugInfo));
            });
        });
    }
}
