package mindustry.io.versions;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.mod.data.*;

import java.io.*;

import static mindustry.Vars.*;

/** Changes data patches to be read before content, and adds support for more complex data patch IO. */
public class Save12 extends SaveVersion{

    public Save12(){
        super(12);
    }

    @Override
    public void skipDataPatches(DataInput stream) throws IOException{
        stream.readInt(); //version - ignored for now
        int amount = stream.readInt();
        for(int i = 0; i < amount; i++){
            int len = stream.readInt();
            stream.skipBytes(len);
        }

        int imageAmount = stream.readInt();
        for(int i = 0; i < imageAmount; i++){
            stream.readUTF(); //name
            stream.skipBytes(4); //w h
            int len = stream.readInt(); //byte data
            stream.skipBytes(len);
        }
    }

    @Override
    public void readDataPatches(DataInput stream) throws IOException{
        stream.readInt(); //version - ignored for now

        Seq<DataAsset> assets = new Seq<>();

        int patchAmount = stream.readInt();
        for(int i = 0; i < patchAmount; i++){
            int len = stream.readInt();
            byte[] bytes = new byte[len];
            stream.readFully(bytes);
            assets.add(new PatchAsset(new String(bytes, Strings.utf8)));
        }

        int imageAmount = stream.readInt();
        for(int i = 0; i < imageAmount; i++){
            String name = stream.readUTF();
            short w = stream.readShort(), h = stream.readShort();
            byte[] bytes = new byte[stream.readInt()];
            stream.readFully(bytes);
            assets.add(new ImageAsset(name, w, h, bytes));
        }

        Events.fire(new DataPatchLoadEvent(assets));

        state.data.load(assets);
    }
}
