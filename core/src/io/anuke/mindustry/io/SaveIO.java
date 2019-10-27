package io.anuke.mindustry.io;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.io.CounterInputStream;
import io.anuke.arc.util.io.FastDeflaterOutputStream;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.versions.*;
import io.anuke.mindustry.world.WorldContext;

import java.io.*;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

public class SaveIO{
    /** Format header. This is the string 'MSAV' in ASCII. */
    public static final byte[] header = {77, 83, 65, 86};
    public static final IntMap<SaveVersion> versions = new IntMap<>();
    public static final Array<SaveVersion> versionArray = Array.with(new Save1(), new Save2(), new Save3());

    static{
        for(SaveVersion version : versionArray){
            versions.put(version.version, version);
        }
    }

    public static SaveVersion getSaveWriter(){
        return versionArray.peek();
    }

    public static SaveVersion getSaveWriter(int version){
        return versions.get(version);
    }

    public static void save(FileHandle file){
        boolean exists = file.exists();
        if(exists) file.moveTo(backupFileFor(file));
        try{
            write(file);
        }catch(Exception e){
            if(exists) backupFileFor(file).moveTo(file);
            throw new RuntimeException(e);
        }
    }

    public static DataInputStream getStream(FileHandle file){
        return new DataInputStream(new InflaterInputStream(file.read(bufferSize)));
    }

    public static DataInputStream getBackupStream(FileHandle file){
        return new DataInputStream(new InflaterInputStream(backupFileFor(file).read(bufferSize)));
    }

    public static boolean isSaveValid(FileHandle file){
        try{
            return isSaveValid(new DataInputStream(new InflaterInputStream(file.read(bufferSize))));
        }catch(Exception e){
            return false;
        }
    }

    public static boolean isSaveValid(DataInputStream stream){
        try{
            getMeta(stream);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static SaveMeta getMeta(FileHandle file){
        try{
            return getMeta(getStream(file));
        }catch(Exception e){
            return getMeta(getBackupStream(file));
        }
    }

    public static SaveMeta getMeta(DataInputStream stream){

        try{
            readHeader(stream);
            int version = stream.readInt();
            SaveMeta meta = versions.get(version).getMeta(stream);
            stream.close();
            return meta;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static FileHandle fileFor(int slot){
        return saveDirectory.child(slot + "." + Vars.saveExtension);
    }

    public static FileHandle backupFileFor(FileHandle file){
        return file.sibling(file.name() + "-backup." + file.extension());
    }

    public static void write(FileHandle file, StringMap tags){
        write(new FastDeflaterOutputStream(file.write(false, bufferSize)), tags);
    }

    public static void write(FileHandle file){
        write(file, null);
    }

    public static void write(OutputStream os, StringMap tags){
        try(DataOutputStream stream = new DataOutputStream(os)){
            stream.write(header);
            stream.writeInt(getVersion().version);
            if(tags == null){
                getVersion().write(stream);
            }else{
                getVersion().write(stream, tags);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void load(FileHandle file) throws SaveException{
        load(file, world.context);
    }

    public static void load(FileHandle file, WorldContext context) throws SaveException{
        try{
            //try and load; if any exception at all occurs
            load(new InflaterInputStream(file.read(bufferSize)), context);
        }catch(SaveException e){
            e.printStackTrace();
            FileHandle backup = file.sibling(file.name() + "-backup." + file.extension());
            if(backup.exists()){
                load(new InflaterInputStream(backup.read(bufferSize)), context);
            }else{
                throw new SaveException(e.getCause());
            }
        }
    }

    /** Loads from a deflated (!) input stream.*/
    public static void load(InputStream is, WorldContext context) throws SaveException{
        try(CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            logic.reset();
            readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = versions.get(version);

            ver.read(stream, counter, context);
        }catch(Exception e){
            throw new SaveException(e);
        }finally{
            world.setGenerating(false);
            content.setTemporaryMapper(null);
        }
    }

    public static SaveVersion getVersion(){
        return versionArray.peek();
    }

    public static void readHeader(DataInput input) throws IOException{
        byte[] bytes = new byte[header.length];
        input.readFully(bytes);
        if(!Arrays.equals(bytes, header)){
            throw new IOException("Incorrect header! Expecting: " + Arrays.toString(header) + "; Actual: " + Arrays.toString(bytes));
        }
    }

    public static class SaveException extends RuntimeException{
        public SaveException(Throwable throwable){
            super(throwable);
        }
    }
}
