package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Log.LogHandler;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class DebugFragment extends Fragment {
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
    public void build(Group parent){

        Player player = players[0];
        new table(){{
           visible(() -> debug);

           abottom().aleft();

           new table("pane"){{
               defaults().fillX().width(100f);

               new label("Debug");
               row();
               new button("noclip", "toggle", () -> noclip = !noclip);
               row();
               new button("items", () -> {
                   for (int i = 0; i < 10; i++) {
                       ItemDrop.create(Item.all().random(), 5, player.x, player.y, Mathf.random(360f));
                   }
               });
               row();
               new button("team", "toggle", player::toggleTeam);
               row();
               new button("blocks", "toggle", () -> showBlockDebug = !showBlockDebug);
               row();
               new button("fog", () -> showFog = !showFog);
               row();
               new button("gameover", () ->{
                   state.teams.get(Team.blue).cores.get(0).entity.health = 0;
                   state.teams.get(Team.blue).cores.get(0).entity.damage(1);
               });
               row();
               new button("wave", () -> state.wavetime = 0f);
               row();
               new button("death", () -> player.damage(99999, true));
               row();
               new button("spawn", () -> {
                   FloatingDialog dialog = new FloatingDialog("debug spawn");
                   for(UnitType type : UnitType.all()){
                       dialog.content().addImageButton("white", 40, () -> {
                           dialog.hide();
                           BaseUnit unit = type.create(player.getTeam());
                           unit.inventory.addAmmo(type.weapon.getAmmoType(type.weapon.getAcceptedItems().iterator().next()));
                           unit.setWave();
                           unit.set(player.x, player.y);
                           unit.add();
                       }).get().getStyle().imageUp = new TextureRegionDrawable(type.iconRegion);
                   }
                   dialog.addCloseButton();
                   dialog.setFillParent(false);
                   dialog.show();
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
        for (String string : strings) {
            builder.append(string);
            builder.append("\n");
        }
        return builder;
    }
}
