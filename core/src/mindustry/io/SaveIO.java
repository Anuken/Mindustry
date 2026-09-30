package mindustry.io;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.io.versions.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class SaveIO{
    /** Save format header. */
    public static final byte[] header = {'M', 'S', 'A', 'V'};
    public static final IntMap<SaveVersion> versions = new IntMap<>();
    public static final Seq<SaveVersion> versionArray = Seq.with(new Save1(), new Save2(), new Save3(), new Save4(), new Save5(), new Save6(), new Save7(), new Save8(), new Save9(), new Save10(), new Save11(), new Save12(), new Save13());

    static{
        for(SaveVersion version : versionArray){
            versions.put(version.version, version);
        }
    }

    public static SaveVersion getSaveWriter(){
        return versionArray.peek();
    }

    public static @Nullable SaveVersion getSaveWriter(int version){
        return versions.get(version);
    }

    public static void save(Fi file){
        save(file, new SaveOptions());
    }

    public static void save(Fi file, SaveOptions options){
        boolean exists = file.exists();
        if(exists) file.moveTo(backupFileFor(file));
        try{
            write(file, options);
        }catch(Throwable e){
            if(exists) backupFileFor(file).moveTo(file);
            throw new RuntimeException(e);
        }
    }

    public static DataInputStream getStream(Fi file){
        return new DataInputStream(new InflaterInputStream(file.read(bufferSize)));
    }

    public static DataInputStream getBackupStream(Fi file){
        return new DataInputStream(new InflaterInputStream(backupFileFor(file).read(bufferSize)));
    }

    public static boolean isSaveValid(Fi file){
        return isSaveFileValid(file) || isSaveFileValid(backupFileFor(file));
    }

    private static boolean isSaveFileValid(Fi file){
        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(file.read(bufferSize)))){
            getMeta(stream);
            return true;
        }catch(Throwable e){
            return false;
        }
    }

    public static boolean isSaveValid(DataInputStream stream){
        try{
            getMeta(stream);
            return true;
        }catch(Throwable e){
            return false;
        }
    }

    public static SaveMeta getMeta(Fi file){
        try{
            return getMeta(getStream(file));
        }catch(Throwable e){
            Log.err(e);
            return getMeta(getBackupStream(file));
        }
    }

    public static SaveMeta getMeta(DataInputStream stream){

        try{
            readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = versions.get(version);

            if(ver == null) throw new IOException("Unknown save version: " + version + ". Are you trying to load a save from a newer version?");

            SaveMeta meta = ver.getMeta(stream);
            stream.close();
            return meta;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Fi fileFor(int slot){
        return saveDirectory.child(slot + "." + Vars.saveExtension);
    }

    public static Fi backupFileFor(Fi file){
        return file.sibling(file.name() + "-backup." + file.extension());
    }

    public static void write(Fi file, SaveOptions options) throws Throwable{
        write(new FastDeflaterOutputStream(file.write(false, bufferSize)), options);
    }

    public static void write(Fi file) throws Throwable{
        write(file, new SaveOptions());
    }

    public static void write(OutputStream os, SaveOptions options) throws Throwable{
        try(DataOutputStream stream = new DataOutputStream(os)){
            Events.fire(new SaveWriteEvent());
            SaveVersion ver = getVersion();

            stream.write(header);
            stream.writeInt(ver.version);
            ver.write(stream, options);
        }
    }

    //TODO: add this back after reviewing callsites
    //public static void load(String saveName) throws SaveLoadException{
    //    load(saveDirectory.child(saveName + ".msav"));
   // }

    //TODO: add this back after reviewing callsites (make sure context is correct)
    //public static void load(Fi file) throws SaveLoadException{
    //    load(file, new DefaultWorldContext(Vars.state));
    //}

    public static void load(Fi file, SaveLoadContext context) throws SaveLoadException{
        try{
            //try and load; if any exception at all occurs
            load(new InflaterInputStream(file.read(bufferSize)), context);
        }catch(SaveLoadException e){
            Log.err(e);
            Fi backup = file.sibling(file.name() + "-backup." + file.extension());
            if(backup.exists()){
                load(new InflaterInputStream(backup.read(bufferSize)), context);
            }else{
                throw new SaveLoadException(e.getCause());
            }
        }
    }

    /** Loads from a deflated (!) input stream. */
    public static void load(InputStream is, SaveLoadContext context) throws SaveLoadException{
        try(CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            logic.reset();
            readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = versions.get(version);

            if(ver == null) throw new IOException("Unknown save version: " + version + ". Are you trying to load a save from a newer version?");

            ver.read(stream, counter, context);
            Events.fire(new SaveLoadEvent(context.isMap()));

            //this gets handled elsewhere when starting a new game or loading a sector
            if(!context.isMap() && !state.isCampaign()){
                Events.fire(new RulesLoadEvent(state.rules, true));
            }
        }catch(Throwable e){
            throw new SaveLoadException(e);
        }finally{
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

    /** Thrown when map or save loading fails. This is a checked exception; failing a map load should not crash the game! */
    public static class SaveLoadException extends Exception{

        public SaveLoadException(Throwable throwable){
            super(throwable);
        }

        public SaveLoadException(String message){
            super(message);
        }

        public SaveLoadException(String message, Throwable cause){
            super(message, cause);
        }
    }
}
