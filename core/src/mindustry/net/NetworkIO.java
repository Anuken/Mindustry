package mindustry.net;

import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.net.Administration.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import static mindustry.Vars.*;

public class NetworkIO{

    private static StringMap tags = new StringMap();

    public static void writeWorld(Player player, OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){
            stream.writeUTF(JsonIO.write(state.rules));

            tags.clear();
            world.getMap().tags.each((key, value) -> tags.put(key, value));
            tags.put("name", "Modern Caldera");

            SaveIO.getSaveWriter().writeStringMap(stream, tags);

            stream.writeInt(state.wave);
            stream.writeFloat(state.wavetime);

            stream.writeInt(player.id);
            player.write(stream);

            SaveIO.getSaveWriter().writeContentHeader(stream);
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

            entities.clear();
            int id = stream.readInt();
            player.resetNoAdd();
            player.read(stream);
            player.resetID(id);
            player.add();

            SaveIO.getSaveWriter().readContentHeader(stream);
            SaveIO.getSaveWriter().readMap(stream, world.context);
        }catch(IOException e){
            throw new RuntimeException(e);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static ByteBuffer writeServerData(InetAddress address){
        String name = (headless ? Config.name.string() : player.name);
        String map = world.getMap() == null ? "None" : Strings.stripColors(world.getMap().name());
        String description = headless && !Config.desc.string().equals("off") ? Config.desc.string() : "";

        String[] greyscale = {"/127.0.0.1", "/192.99.169.18"};

        if (Arrays.asList(greyscale).contains(address.toString())){
            name = "<:surgealloy:632785242788200461> surgely a nice server to play on?";
            description = Strings.stripColors(description);
        }

        ByteBuffer buffer = ByteBuffer.allocate(512);

        writeString(buffer, name, 100);
        writeString(buffer, map);

        buffer.putInt(playerGroup.size());
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        writeString(buffer, Version.type);

        buffer.put((byte)Gamemode.bestFit(state.rules).ordinal());
        buffer.putInt(netServer.admins.getPlayerLimit());

        writeString(buffer, description, 100);
        return buffer;
    }

    public static Host readServerData(String hostAddress, ByteBuffer buffer){
        String host = readString(buffer);
        String map = readString(buffer);
        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();
        String vertype = readString(buffer);
        Gamemode gamemode = Gamemode.all[buffer.get()];
        int limit = buffer.getInt();
        String description = readString(buffer);

        return new Host(host, hostAddress, map, wave, players, version, vertype, gamemode, limit, description);
    }

    private static void writeString(ByteBuffer buffer, String string, int maxlen){
        byte[] bytes = string.getBytes(charset);
        //todo truncating this way may lead to wierd encoding errors at the ends of strings...
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
