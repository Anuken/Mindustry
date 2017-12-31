package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.Module;

import java.util.Arrays;

public class NetClient extends Module {
    boolean connecting = false;
    boolean gotEntities = false;
    float playerSyncTime = 3;

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            connecting = true;
            gotEntities = false;
            Gdx.app.postRunnable(() -> {
                Vars.ui.hideLoading();
                Vars.ui.showLoading("$text.connecting.data");
            });
        });

        Net.handle(Disconnect.class, packet -> {
            Gdx.app.postRunnable(() -> {
                Timers.runFor(3f, () -> {
                    Vars.ui.hideLoading();
                });

                Vars.ui.showError("$text.disconnect");
                connecting = false;
            });
        });

        Net.handle(WorldData.class, data -> {
            Gdx.app.postRunnable(() -> {
                UCore.log("Recieved world data: " + data.stream.available() + " bytes.");
                SaveIO.load(data.stream);

                GameState.set(State.playing);
                connecting = false;
                Vars.ui.hideLoading();
                Vars.ui.hideJoinGame();
            });
        });

        Net.handle(EntityDataPacket.class, data -> {

            Gdx.app.postRunnable(() -> {
                Timers.run(10f, () -> { //TODO hack
                    Vars.control.playerGroup.remap(Vars.player, data.playerid);

                    for (Player player : data.players) {
                        if (player.id != data.playerid) {
                            player.add();
                        }
                    }

                    UCore.log("Recieved entities: " + Arrays.toString(data.players) + " player ID: " + data.playerid);
                    gotEntities = true;
                });
            });
        });

        Net.handle(SyncPacket.class, packet -> {
            if(!gotEntities) return;

            for(int i = 0; i < packet.ids.length; i ++){
                int id = packet.ids[i];
                if(id != Vars.player.id){
                    Player player = Vars.control.playerGroup.getByID(id);
                    player.getInterpolator().type.read(player, packet.data[i]);
                }
            }
        });

        Net.handle(ShootPacket.class, packet -> {
            Player player = Vars.control.playerGroup.getByID(packet.playerid);

            Weapon weapon = Weapon.values()[packet.weaponid];
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
        });

        Net.handleServer(PlacePacket.class, packet -> {
            Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);
        });

        Net.handleServer(BreakPacket.class, packet -> {
            Vars.control.input.breakBlockInternal(packet.x, packet.y, false);
        });

        Net.handleServer(StateSyncPacket.class, packet -> {
            //TODO replace with arraycopy()
            for(int i = 0; i < packet.items.length; i ++){
                Vars.control.items[i] = packet.items[i];
            }
            Vars.control.setWaveData(packet.enemies, packet.wave, packet.countdown);
        });
    }

    public void update(){
        if(!Net.client()) return;

        if(!GameState.is(State.menu) && Net.active()){
            sync();
        }else if(!connecting){
            Net.disconnect();
        }
    }

    public void handleShoot(Weapon weapon, float x, float y, float angle){
        ShootPacket packet = new ShootPacket();
        packet.weaponid = (byte)weapon.ordinal();
        packet.x = x;
        packet.y = y;
        packet.rotation = angle;
        Net.send(packet, SendMode.udp);
    }

    public void handlePlace(int x, int y, Block block, int rotation){
        PlacePacket packet = new PlacePacket();
        packet.x = (short)x;
        packet.y = (short)y;
        packet.rotation = (byte)rotation;
        packet.playerid = Vars.player.id;
        packet.block = block.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleBreak(int x, int y){
        BreakPacket packet = new BreakPacket();
        packet.x = (short)x;
        packet.y = (short)y;
        Net.send(packet, SendMode.tcp);
    }

    void sync(){
        if(Timers.get("syncPlayer", playerSyncTime)){
            PositionPacket packet = new PositionPacket();
            packet.data = Vars.player.getInterpolator().type.write(Vars.player);
            Net.send(packet, SendMode.tcp); //TODO udp instead?
        }
    }
}
