package mindustry.net;

import arc.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.maps.Map;
import mindustry.net.Administration.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import static mindustry.Vars.*;

public class NetworkIO{

    public static void writeWorld(Player player, OutputStream os){

        try(DataOutputStream stream = new DataOutputStream(os)){
            //write all researched content to rules if hosting
            if(state.isCampaign()){
                state.rules.researched.clear();
                for(ContentType type : ContentType.all){
                    for(Content c : content.getBy(type)){
                        if(c instanceof UnlockableContent u && u.unlocked() && u.techNode != null){
                            state.rules.researched.add(u.name);
                        }
                    }
                }
            }

            stream.writeUTF(JsonIO.write(state.rules));
            SaveIO.getSaveWriter().writeStringMap(stream, state.map.tags);

            stream.writeInt(state.wave);
            stream.writeFloat(state.wavetime);
            stream.writeDouble(state.tick);
            stream.writeLong(GlobalVars.rand.seed0);
            stream.writeLong(GlobalVars.rand.seed1);

            Writes write = new Writes(stream);

            stream.writeInt(player.id);
            player.write(write);

            boolean any = !state.rules.fog;

            stream.writeInt(any ? Groups.sync.size() : 0);

            if(any){
                //write all synced entities *immediately*
                for(Syncc entity : Groups.sync){
                    stream.writeInt(entity.id());
                    stream.writeByte(entity.classId());
                    entity.writeSync(write);
                }
            }

            SaveIO.getSaveWriter().writeContentHeader(stream);
            SaveIO.getSaveWriter().writeMap(stream);
            SaveIO.getSaveWriter().writeTeamBlocks(stream);
            SaveIO.getSaveWriter().writeCustomChunks(stream, true);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void loadWorld(InputStream is){

        try(DataInputStream stream = new DataInputStream(is)){
            Time.clear();
            state.rules = JsonIO.read(Rules.class, stream.readUTF());
            state.map = new Map(SaveIO.getSaveWriter().readStringMap(stream));

            state.wave = stream.readInt();
            state.wavetime = stream.readFloat();
            state.tick = stream.readDouble();
            GlobalVars.rand.seed0 = stream.readLong();
            GlobalVars.rand.seed1 = stream.readLong();

            Reads read = new Reads(stream);

            Groups.clear();
            int id = stream.readInt();
            player.reset();
            player.read(read);
            player.id = id;
            player.add();

            int entities = stream.readInt();

            for(int j = 0; j < entities; j++){
                NetClient.readSyncEntity(stream, read);
            }

            SaveIO.getSaveWriter().readContentHeader(stream);
            SaveIO.getSaveWriter().readMap(stream, world.context);
            SaveIO.getSaveWriter().readTeamBlocks(stream);
            SaveIO.getSaveWriter().readCustomChunks(stream);
        }catch(IOException e){
            throw new RuntimeException(e);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static ByteBuffer writeServerData(){
        String name = (headless ? Config.serverName.string() : player.name);
        String description = headless && !Config.desc.string().equals("off") ? Config.desc.string() : "";
        String map = state.map.name();

        ByteBuffer buffer = ByteBuffer.allocate(500);

        writeString(buffer, name, 100);
        writeString(buffer, map, 64);

        buffer.putInt(Core.settings.getInt("totalPlayers", Groups.player.size()));
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        writeString(buffer, Version.type);

        buffer.put((byte)state.rules.mode().ordinal());
        buffer.putInt(netServer.admins.getPlayerLimit());

        writeString(buffer, description, 100);
        if(state.rules.modeName != null){
            writeString(buffer, state.rules.modeName, 50);
        }
        return buffer;
    }

    public static Host readServerData(int ping, String hostAddress, ByteBuffer buffer){
        String host = readString(buffer);
        String map = readString(buffer);
        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();
        String vertype = readString(buffer);
        Gamemode gamemode = Gamemode.all[buffer.get()];
        int limit = buffer.getInt();
        String description = readString(buffer);
        String modeName = readString(buffer);

        return new Host(ping, host, hostAddress, map, wave, players, version, vertype, gamemode, limit, description, modeName.isEmpty() ? null : modeName);
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
