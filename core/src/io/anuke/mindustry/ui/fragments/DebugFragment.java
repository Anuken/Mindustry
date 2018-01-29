package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
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
                log.append(Log.format(text, args));
                log.append("\n");
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
               new button("paths", "toggle", () -> showPaths = !showPaths);
               row();
               new button("infammo", "toggle", () -> infiniteAmmo = !infiniteAmmo);
               row();
               new button("wave", () -> logic.runWave());
               row();
               new button("clear", () -> {
                   enemyGroup.clear();
                   state.enemies = 0;
                   netClient.clearRecieved();
               });
               row();
               new button("spawn", () -> new Enemy(EnemyTypes.standard).set(player.x, player.y).add());
               row();
           }}.end();

           row();

        }}.end();


        new table(){{
            visible(() -> console);

            atop().aleft();

            new table("pane") {{
                defaults().fillX();

                new label(DebugFragment::debugInfo);
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

    public static String debugInfo(){
        return join(
                "net.active: " + Net.active(),
                "net.server: " + Net.server(),
                "chat.open: " + ui.chatfrag.chatOpen(),
                "chat.messages: " + ui.chatfrag.getMessagesSize(),
                "players: " + playerGroup.size(),
                "enemies: " + enemyGroup.size(),
                "tiles: " + tileGroup.size(),
                "",
                clientDebug.getOut()
        );
    }

    private static String join(String... strings){
        StringBuilder builder = new StringBuilder();
        for (String string : strings) {
            builder.append(string);
            builder.append("\n");
        }
        return builder.toString();
    }
}
