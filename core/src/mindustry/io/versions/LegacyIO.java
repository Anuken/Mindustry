package mindustry.io.versions;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.ui.dialogs.JoinDialog.*;

import java.io.*;

public class LegacyIO{
    /** Maps old unit names to new ones. */
    public static final StringMap unitMap = StringMap.of(
    "titan", "mace",
    "chaos-array", "scepter",
    "eradicator", "reign",
    "eruptor", "atrax",
    "wraith", "flare",
    "ghoul", "horizon",
    "revenant", "zenith",
    "lich", "antumbra",
    "reaper", "eclipse",
    "draug", "mono",
    "phantom", "poly",
    "spirit", "poly"
    );

    public static Seq<Server> readServers(){
        Seq<Server> arr = new Seq<>();

        try{
            byte[] bytes = Core.settings.getBytes("server-list");
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));

            int length = stream.readInt();
            if(length > 0){
                //name of type, irrelevant
                stream.readUTF();

                for(int i = 0; i < length; i++){
                    Server server = new Server();
                    server.ip = stream.readUTF();
                    server.port = stream.readInt();
                    arr.add(server);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return arr;
    }

    public static void readResearch(){
        try{
            byte[] bytes = Core.settings.getBytes("unlocks");
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));

            int length = stream.readInt();
            if(length > 0){
                stream.readUTF(); //name of key type
                stream.readUTF(); //name of value type

                //each element is an array list
                for(int i = 0; i < length; i++){
                    ContentType type = ContentType.all[stream.readInt()];
                    int arrLength = stream.readInt();
                    if(arrLength > 0){
                        stream.readUTF(); //type of contents (String)
                        for(int j = 0; j < arrLength; j++){
                            String name = stream.readUTF();
                            Content out = Vars.content.getByName(type, name);
                            if(out instanceof UnlockableContent u){
                                u.quietUnlock();
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            Log.err(e);
        }
    }
}
