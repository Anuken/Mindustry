package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.versions.Save16;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

public class SaveIO{
    public static final IntArray breakingVersions = IntArray.with(47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 63);
    public static final IntMap<SaveFileVersion> versions = new IntMap<>();
    public static final Array<SaveFileVersion> versionArray = Array.with(
        new Save16()
    );

    static{
        for(SaveFileVersion version : versionArray){
            versions.put(version.version, version);
        }
    }

    public static SaveFileVersion getSaveWriter(){
        return versionArray.peek();
    }

    public static void saveToSlot(int slot){
        FileHandle file = fileFor(slot);
        boolean exists = file.exists();
        if(exists) file.moveTo(file.sibling(file.name() + "-backup." + file.extension()));
        try{
            write(fileFor(slot));
        }catch(Exception e){
            if(exists) file.sibling(file.name() + "-backup." + file.extension()).moveTo(file);
            throw new RuntimeException(e);
        }
    }

    public static void loadFromSlot(int slot){
        load(fileFor(slot));
    }

    public static DataInputStream getSlotStream(int slot){
        return new DataInputStream(new InflaterInputStream(fileFor(slot).read()));
    }

    public static boolean isSaveValid(int slot){
        try{
            return isSaveValid(getSlotStream(slot));
        }catch(Exception e){
            return false;
        }
    }

    public static boolean isSaveValid(FileHandle file){
        return isSaveValid(new DataInputStream(new InflaterInputStream(file.read())));
    }

    public static boolean isSaveValid(DataInputStream stream){

        try{
            int version = stream.readInt();
            SaveFileVersion ver = versions.get(version);
            ver.getData(stream);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static SaveMeta getData(int slot){
        return getData(getSlotStream(slot));
    }

    public static SaveMeta getData(DataInputStream stream){

        try{
            int version = stream.readInt();
            SaveMeta meta = versions.get(version).getData(stream);
            stream.close();
            return meta;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static FileHandle fileFor(int slot){
        return saveDirectory.child(slot + "." + Vars.saveExtension);
    }

    public static void write(FileHandle file){
        write(new DeflaterOutputStream(file.write(false)){
            byte[] tmp = {0};

            public void write(int var1) throws IOException {
                tmp[0] = (byte)(var1 & 255);
                this.write(tmp, 0, 1);
            }
        });
    }

    public static void write(OutputStream os){
        DataOutputStream stream;

        try{
            stream = new DataOutputStream(os);
            getVersion().write(stream);
            stream.close();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void load(FileHandle file){
        try{
            load(new InflaterInputStream(file.read()));
        }catch(RuntimeException e){
            e.printStackTrace();
            FileHandle backup = file.sibling(file.name() + "-backup." + file.extension());
            if(backup.exists()){
                load(new InflaterInputStream(backup.read()));
            }else{
                throw new RuntimeException(e);
            }
        }
    }

    public static void load(InputStream is){
        logic.reset();

        DataInputStream stream;

        try{
            stream = new DataInputStream(is);
            int version = stream.readInt();
            SaveFileVersion ver = versions.get(version);

            ver.read(stream);

            stream.close();
        }catch(Exception e){
            content.setTemporaryMapper(null);
            throw new RuntimeException(e);
        }
    }

    public static SaveFileVersion getVersion(){
        return versionArray.peek();
    }
}
