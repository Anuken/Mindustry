package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Log.LogHandler;

import static io.anuke.mindustry.Vars.*;

public class DebugFragment implements Fragment {
    private static StringBuilder log = new StringBuilder();

    static{
        Log.setLogger(new LogHandler(){
            @Override
            public void print(String text, Object... args){
                super.print(text, args);
                if(log.length() < 1000) {
                    log.append(Log.format(text, args));
                    log.append("\n");
                }
            }
        });
    }

    @Override
    public void build(){
        new table(){{
           visible(() -> debug);

           atop().aright();

           new table("pane"){{
               defaults().fillX();

               new label("Debug");
               row();
               new button("noclip", "toggle", () -> noclip = !noclip);
               row();
               new button("hideplayer", "toggle", () -> showPlayer = !showPlayer);
               row();
               new button("blocks", "toggle", () -> showBlockDebug = !showBlockDebug);
               row();
               new button("paths", "toggle", () -> showPaths = !showPaths);
               row();
               new button("wave", () -> state.wavetime = 0f);
               row();
               new button("time 0", () -> Timers.resetTime(0f));
               row();
               new button("time max", () -> Timers.resetTime(1080000 - 60*10));
               row();
               new button("clear", () -> {
                   enemyGroup.clear();
                   state.enemies = 0;
                   netClient.clearRecieved();
               });
               row();
               new button("spawn", () -> {
                   new Enemy(EnemyTypes.healer).set(player.x, player.y).add();
               });
               row();
           }}.end();

           row();

        }}.end();


        new table(){{
            visible(() -> console);

            atop().aleft();

            new table("pane") {{
                defaults().fillX();

                ScrollPane pane = new ScrollPane(new Label(DebugFragment::debugInfo), "clear");

                add(pane);
                row();
                new button("dump", () -> {
                    try{
                        FileHandle file = Gdx.files.local("packet-dump.txt");
                        file.writeString("--INFO--\n", false);
                        file.writeString(debugInfo(), true);
                        file.writeString("--LOG--\n\n", true);
                        file.writeString(log.toString(), true);
                    }catch (Exception e){
                        ui.showError("Error dumping log.");
                    }
                });
            }}.end();
        }}.end();

        new table(){{
            visible(() -> console);

            atop();

            Table table = new Table("pane");
            table.label(() -> log.toString());

            ScrollPane pane = new ScrollPane(table, "clear");

            get().add(pane);
        }}.end();
    }

    public static void printDebugInfo(){
        Gdx.app.error("Minudstry Info Dump", debugInfo());
    }

    public static String debugInfo(){
        StringBuilder result = join(
                "net.active: " + Net.active(),
                "net.server: " + Net.server(),
                "net.client: " + Net.client(),
                "state: " + state.getState(),
                Net.client() ?
                "chat.open: " + ui.chatfrag.chatOpen() + "\n" +
                "chat.messages: " + ui.chatfrag.getMessagesSize() + "\n" +
                "client.connecting: " + netClient.isConnecting() + "\n" : "",
                "players: " + playerGroup.size(),
                "enemies: " + enemyGroup.size(),
                "tiles: " + tileGroup.size(),
                "time: " + Timers.time(),
                world.getCore() != null && world.getCore().entity != null ? "core.health: " + world.getCore().entity.health : "",
                "core: " + world.getCore(),
                "state.gameover: " + state.gameOver,
                "state: " + state.getState(),
                !Net.server() ? clientDebug.getOut() : serverDebug.getOut()
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
            result.append(player.clientid);
            result.append("\n");
            result.append("   dead: ");
            result.append(player.isDead());
            result.append("\n");
            result.append("   pos: ");
            result.append(player.x);
            result.append(", ");
            result.append(player.y);
            result.append("\n");
            result.append("   android: ");
            result.append(player.isAndroid);
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
        for (String string : strings) {
            builder.append(string);
            builder.append("\n");
        }
        return builder;
    }
}
