package io.anuke.mindustry.net;

import io.anuke.arc.Core;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.JsonIO;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.Map;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class NetworkIO{

    public static void writeWorld(Player player, OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){
            stream.writeUTF(JsonIO.write(state.rules));
            SaveIO.getSaveWriter().writeStringMap(stream, world.getMap().tags);

            stream.writeInt(state.wave);
            stream.writeFloat(state.wavetime);

            stream.writeInt(state.round);
            stream.writeFloat(state.eliminationtime);

            for(int i=0; i<state.points.length; i++){
                stream.writeInt(state.points[i]);
            }

            stream.writeInt(player.id);
            player.write(stream);

            SaveIO.getSaveWriter().writeMap(stream);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void loadWorld(InputStream is){

        try(DataInputStream stream = new DataInputStream(is)){
            Time.clear();
            state.rules = JsonIO.read(Rules.class, stream.readUTF());
            world.setMap(new Map(SaveIO.getSaveWriter().readStringMap(stream)));

            state.wave = stream.readInt();
            state.wavetime = stream.readFloat();

            state.round = stream.readInt();
            state.eliminationtime = stream.readFloat();

            for(int i=0; i<state.points.length; i++){
                state.points[i] = stream.readInt();
            }

            Entities.clear();
            int id = stream.readInt();
            player.resetNoAdd();
            player.read(stream);
            player.resetID(id);
            player.add();

            SaveIO.getSaveWriter().readMap(stream, world.context);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer writeServerData(){
        String name = (headless ? Core.settings.getString("servername") : player.name);
        String map = world.getMap() == null ? "None" : world.getMap().name();

        ByteBuffer buffer = ByteBuffer.allocate(256);

        writeString(buffer, name, 100);
        writeString(buffer, map);

        buffer.putInt(playerGroup.size());
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        writeString(buffer, Version.type);
        //TODO additional information:
        // - gamemode ID/name (just pick the closest one?)
        return buffer;
    }

    public static Host readServerData(String hostAddress, ByteBuffer buffer){
        String host = readString(buffer);
        String map = readString(buffer);
        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();
        String vertype = readString(buffer);

        return new Host(host, hostAddress, map, wave, players, version, vertype);
    }

    private static void writeString(ByteBuffer buffer, String string, int maxlen){
        byte[] bytes = string.getBytes(charset);
        //truncating this way may lead to wierd encoding errors at the ends of strings...
        if(bytes.length > maxlen){
            bytes = Arrays.copyOfRange(bytes, 0, maxlen);
        }

        buffer.put((byte)bytes.length);
        buffer.put(bytes);
    }

    private static void writeString(ByteBuffer buffer, String string){
        writeString(buffer, string, 32);
    }

    private static String readString(ByteBuffer buffer){
        short length = (short)(buffer.get() & 0xff);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, charset);
    }
}
