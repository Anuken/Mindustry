package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Bits;

import java.io.*;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class NetworkIO {

    public static void writeWorld(Player player, OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){

            stream.writeFloat(Timers.time()); //timer time
            stream.writeLong(TimeUtils.millis()); //timestamp

            //--GENERAL STATE--
            stream.writeByte(state.mode.ordinal()); //gamemode
            stream.writeUTF(world.getMap().name); //map ID

            stream.writeInt(state.wave); //wave
            stream.writeFloat(state.wavetime); //wave countdown
            stream.writeInt(state.enemies); //enemy amount

            stream.writeBoolean(state.friendlyFire); //friendly fire state
            stream.writeInt(player.id); //player remap ID
            stream.writeBoolean(player.isAdmin);

            stream.writeByte(player.upgrades.size);
            for(Upgrade u : player.upgrades){
                stream.writeByte(u.id);
            }

            //--MAP DATA--

            //map size
            stream.writeShort(world.width());
            stream.writeShort(world.height());

            for(int x = 0; x < world.width(); x ++){
                for(int y = 0; y < world.height(); y ++){
                    Tile tile = world.tile(x, y);

                    //TODO will break if block number gets over BYTE_MAX
                    stream.writeByte(tile.floor().id); //floor ID
                    stream.writeByte(tile.block().id); //block ID

                    if(tile.block() instanceof BlockPart){
                        stream.writeByte(tile.link);
                    }

                    if(tile.entity != null){
                        stream.writeByte(Bits.packByte((byte)tile.getTeam().ordinal(), tile.getRotation()));
                        stream.writeShort((short)tile.entity.health); //health

                        if(tile.entity.items != null) tile.entity.items.write(stream);
                        if(tile.entity.power != null) tile.entity.power.write(stream);
                        if(tile.entity.liquids != null) tile.entity.liquids.write(stream);

                        tile.entity.write(stream);
                    }

                }
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**Return whether a custom map is expected, and thus whether the client should wait for additional data.*/
    public static void loadWorld(InputStream is){

        Player player = players[0];

        //TODO !! use map name as the network map in Maps, so getMap() isn't null.

        try(DataInputStream stream = new DataInputStream(is)){
            float timerTime = stream.readFloat();
            long timestamp = stream.readLong();

            Timers.clear();
            Timers.resetTime(timerTime + (TimeUtils.timeSinceMillis(timestamp) / 1000f) * 60f);

            //general state
            byte mode = stream.readByte();
            String map = stream.readUTF();

            int wave = stream.readInt();
            float wavetime = stream.readFloat();
            int enemies = stream.readInt();
            boolean friendlyfire = stream.readBoolean();

            state.enemies = enemies;
            state.wave = wave;
            state.wavetime = wavetime;
            state.mode = GameMode.values()[mode];
            state.friendlyFire = friendlyfire;

            int pid = stream.readInt();
            boolean admin = stream.readBoolean();

            byte weapons = stream.readByte();

            for(int i = 0; i < weapons; i ++){
                player.upgrades.add(Upgrade.getByID(stream.readByte()));
            }

            player.weapon = Weapons.blaster;

            Entities.clear();
            player.id = pid;
            player.isAdmin = admin;
            player.add();

            world.beginMapLoad();

            //map
            int width = stream.readShort();
            int height = stream.readShort();

            Tile[][] tiles = world.createTiles(width, height);

            for(int x = 0; x < world.width(); x ++){
                for(int y = 0; y < world.height(); y ++){
                    byte floorid = stream.readByte();
                    byte blockid = stream.readByte();

                    Tile tile = new Tile(x, y, floorid, blockid);

                    if(tile.block() == Blocks.blockpart){
                        tile.link = stream.readByte();
                    }

                    if(tile.entity != null) {
                        byte tr = stream.readByte();
                        short health = stream.readShort();

                        tile.setTeam(Team.values()[Bits.getLeftByte(tr)]);
                        tile.setRotation(Bits.getRightByte(tr));

                        tile.entity.health = health;

                        if (tile.entity.items != null) tile.entity.items.read(stream);
                        if (tile.entity.power != null) tile.entity.power.read(stream);
                        if (tile.entity.liquids != null) tile.entity.liquids.read(stream);

                        tile.entity.read(stream);
                    }

                    tiles[x][y] = tile;
                }
            }

            player.dead = true;

            world.endMapLoad();

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer writeServerData(){
        int maxlen = 32;

        String host = (headless ? "Server" : players[0].name);
        String map = world.getMap().name;

        host = host.substring(0, Math.min(host.length(), maxlen));
        map = map.substring(0, Math.min(map.length(), maxlen));

        ByteBuffer buffer = ByteBuffer.allocate(128);

        buffer.put((byte)host.getBytes().length);
        buffer.put(host.getBytes());

        buffer.put((byte)map.getBytes().length);
        buffer.put(map.getBytes());

        buffer.putInt(playerGroup.size());
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        return buffer;
    }

    public static Host readServerData(String hostAddress, ByteBuffer buffer){
        byte hlength = buffer.get();
        byte[] hb = new byte[hlength];
        buffer.get(hb);

        byte mlength = buffer.get();
        byte[] mb = new byte[mlength];
        buffer.get(mb);

        String host = new String(hb);
        String map = new String(mb);

        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();

        return new Host(host, hostAddress, map, wave, players, version);
    }
}
