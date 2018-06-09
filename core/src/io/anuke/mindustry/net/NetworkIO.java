package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.io.Map;
import io.anuke.mindustry.io.MapMeta;
import io.anuke.mindustry.io.Version;
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
            stream.writeUTF(world.getMap().name); //map name

            stream.writeInt(state.wave); //wave
            stream.writeFloat(state.wavetime); //wave countdown

            stream.writeBoolean(state.friendlyFire); //friendly fire state

            stream.writeInt(player.id);
            player.write(stream);

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

            //write team data
            stream.writeByte(state.teams.getTeams().size);
            for(TeamData data : state.teams.getTeams()){
                stream.writeByte(data.team.ordinal());
                stream.writeBoolean(data.ally);
                stream.writeShort(data.cores.size);
                for(Tile tile : data.cores){
                    stream.writeInt(tile.packedPosition());
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

            boolean friendlyfire = stream.readBoolean();

            state.wave = wave;
            state.wavetime = wavetime;
            state.mode = GameMode.values()[mode];
            state.friendlyFire = friendlyfire;

            Entities.clear();
            int id = stream.readInt();
            player.read(stream, TimeUtils.millis());
            player.resetID(id);
            player.add();

            world.beginMapLoad();

            //map
            int width = stream.readShort();
            int height = stream.readShort();

            //TODO send advanced map meta such as author, etc
            //TODO scan for cores
            Map currentMap = new Map(map, new MapMeta(0, new ObjectMap<>(), width, height, null), true, () -> null);
            world.setMap(currentMap);

            Tile[][] tiles = world.createTiles(width, height);

            for(int x = 0; x < width; x ++){
                for(int y = 0; y < height; y ++){
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

            player.reset();
            state.teams = new TeamInfo();

            byte teams = stream.readByte();
            for (int i = 0; i < teams; i++) {
                Team team = Team.values()[stream.readByte()];
                boolean ally = stream.readBoolean();
                short cores = stream.readShort();
                state.teams.add(team, ally);

                for (int j = 0; j < cores; j++) {
                    state.teams.get(team).cores.add(world.tile(stream.readInt()));
                }
            }

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
