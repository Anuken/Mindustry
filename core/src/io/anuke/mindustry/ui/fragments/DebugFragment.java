package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.ui.dialogs.GenViewDialog;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Log.LogHandler;

import static io.anuke.mindustry.Vars.*;

public class DebugFragment extends Fragment{
    private static StringBuilder log = new StringBuilder();

    static{
        Log.setLogger(new LogHandler(){
            @Override
            public void print(String text, Object... args){
                super.print(text, args);
                if(log.length() < 1000){
                    log.append(Log.format(text, args));
                    log.append("\n");
                }
            }
        });
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
        for(String string : strings){
            builder.append(string);
            builder.append("\n");
        }
        return builder;
    }

    @Override
    public void build(Group parent){

        Player player = players[0];
        parent.fill(c -> {
            c.bottom().left().visible(() -> debug);

            c.table("pane", t -> {
                t.defaults().fillX().width(100f);

                t.label(() -> Gdx.app.getJavaHeap() / 1024 / 1024 + "MB");
                t.row();

                t.add("Debug");
                t.row();
                t.addButton("map", () -> new GenViewDialog().show());
                t.row();
                t.addButton("noclip", "toggle", () -> noclip = !noclip);
                t.row();
                t.addButton("team", "toggle", player::toggleTeam);
                t.row();
                t.addButton("blocks", "toggle", () -> showBlockDebug = !showBlockDebug);
                t.row();
                t.addButton("fog", () -> showFog = !showFog);
                t.row();
                t.addButton("gameover", () -> {
                    state.teams.get(Team.blue).cores.get(0).entity.health = 0;
                    state.teams.get(Team.blue).cores.get(0).entity.damage(1);
                });
                t.row();
                t.addButton("wave", () -> state.wavetime = 0f);
                t.row();
                t.addButton("death", () -> player.damage(99999, true));
                t.row();
                t.addButton("spawn", () -> {
                    FloatingDialog dialog = new FloatingDialog("debug spawn");
                    for(UnitType type : UnitType.all()){
                        dialog.content().addImageButton("white", 40, () -> {
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
                t.row();
            });
        });


        parent.fill(t -> {
            t.top().left().visible(() -> console);

            t.table("pane", p -> {
                p.defaults().fillX();

                p.pane("clear", new Label(DebugFragment::debugInfo));
                p.row();
                p.addButton("dump", () -> {
                    try{
                        FileHandle file = Gdx.files.local("packet-dump.txt");
                        file.writeString("--INFO--\n", false);
                        file.writeString(debugInfo(), true);
                        file.writeString("--LOG--\n\n", true);
                        file.writeString(log.toString(), true);
                    }catch(Exception e){
                        ui.showError("Error dumping log.");
                    }
                });
            });
        });

        parent.fill(t -> {
            t.top().visible(() -> console);

            Table table = new Table("pane");
            table.label(() -> log.toString());

            t.pane("clear", table);
        });
    }
}
