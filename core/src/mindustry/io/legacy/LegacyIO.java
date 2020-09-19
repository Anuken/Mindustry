package mindustry.io.legacy;

import arc.*;
import arc.struct.*;
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

}
