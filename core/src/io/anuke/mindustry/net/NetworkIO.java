package io.anuke.mindustry.net;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.Tile;

import java.io.*;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class NetworkIO{

    public static void writeWorld(Player player, OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){
            //--GENERAL STATE--
            Serialization.writeRules(stream, state.rules);
            stream.writeUTF(world.getMap().name); //map name

            //write tags
            ObjectMap<String, String> tags = world.getMap().meta.tags;
            stream.writeByte(tags.size);
            for(Entry<String, String> entry : tags.entries()){
                stream.writeUTF(entry.key);
                stream.writeUTF(entry.value);
            }

            stream.writeInt(state.wave); //wave
            stream.writeFloat(state.wavetime); //wave countdown

            stream.writeInt(player.id);
            player.write(stream);

            world.spawner.write(stream);
            SaveIO.getSaveWriter().writeMap(stream);

            stream.write(Team.all.length);

            //write team data
            for(Team team : Team.all){
                TeamData data = state.teams.get(team);
                stream.writeByte(team.ordinal());

                stream.writeByte(data.enemies.size());
                for(Team enemy : data.enemies){
                    stream.writeByte(enemy.ordinal());
                }

                stream.writeByte(data.cores.size);
                for(Tile tile : data.cores){
                    stream.writeInt(tile.pos());
                }
            }

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void loadWorld(InputStream is){

        Player player = players[0];

        try(DataInputStream stream = new DataInputStream(is)){
            Time.clear();

            //general state
            state.rules = Serialization.readRules(stream);
            String map = stream.readUTF();

            ObjectMap<String, String> tags = new ObjectMap<>();

            byte tagSize = stream.readByte();
            for(int i = 0; i < tagSize; i++){
                String key = stream.readUTF();
                String value = stream.readUTF();
                tags.put(key, value);
            }

            int wave = stream.readInt();
            float wavetime = stream.readFloat();

            state.wave = wave;
            state.wavetime = wavetime;

            Entities.clear();
            int id = stream.readInt();
            player.resetNoAdd();
            player.read(stream);
            player.resetID(id);
            player.add();

            //map
            world.spawner.read(stream);
            SaveIO.getSaveWriter().readMap(stream);
            world.setMap(new Map(map, 0, 0));

            state.teams = new Teams();

            byte teams = stream.readByte();
            for(int i = 0; i < teams; i++){
                Team team = Team.all[stream.readByte()];

                byte enemies = stream.readByte();
                Team[] enemyArr = new Team[enemies];
                for(int j = 0; j < enemies; j++){
                    enemyArr[j] = Team.all[stream.readByte()];
                }

                state.teams.add(team, enemyArr);

                byte cores = stream.readByte();

                for(int j = 0; j < cores; j++){
                    state.teams.get(team).cores.add(world.tile(stream.readInt()));
                }

                if(team == players[0].getTeam() && cores > 0){
                    Core.camera.position.set(state.teams.get(team).cores.first().drawx(), state.teams.get(team).cores.first().drawy());
                }
            }

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer writeServerData(){
        int maxlen = 32;

        String host = (headless ? "Server" : players[0].name);
        String map = world.getMap() == null ? "None" : world.getMap().name;

        host = host.substring(0, Math.min(host.length(), maxlen));
        map = map.substring(0, Math.min(map.length(), maxlen));

        ByteBuffer buffer = ByteBuffer.allocate(128);

        buffer.put((byte) host.getBytes(charset).length);
        buffer.put(host.getBytes(charset));

        buffer.put((byte) map.getBytes(charset).length);
        buffer.put(map.getBytes(charset));

        buffer.putInt(playerGroup.size());
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        buffer.put((byte)Version.type.getBytes(charset).length);
        buffer.put(Version.type.getBytes(charset));
        return buffer;
    }

    public static Host readServerData(String hostAddress, ByteBuffer buffer){
        byte hlength = buffer.get();
        byte[] hb = new byte[hlength];
        buffer.get(hb);

        byte mlength = buffer.get();
        byte[] mb = new byte[mlength];
        buffer.get(mb);

        String host = new String(hb, charset);
        String map = new String(mb, charset);

        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();
        byte tlength = buffer.get();
        byte[] tb = new byte[tlength];
        buffer.get(tb);
        String vertype = new String(tb, charset);

        return new Host(host, hostAddress, map, wave, players, version, vertype);
    }
}
