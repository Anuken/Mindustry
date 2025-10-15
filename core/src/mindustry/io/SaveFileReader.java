package mindustry.io;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.io.*;
import mindustry.world.*;

import java.io.*;

public abstract class SaveFileReader{
    public static final ObjectMap<String, String> fallback = ObjectMap.of(
    "dart-mech-pad", "legacy-mech-pad",
    "dart-ship-pad", "legacy-mech-pad",
    "javelin-ship-pad", "legacy-mech-pad",
    "trident-ship-pad", "legacy-mech-pad",
    "glaive-ship-pad", "legacy-mech-pad",
    "alpha-mech-pad", "legacy-mech-pad",
    "tau-mech-pad", "legacy-mech-pad",
    "omega-mech-pad", "legacy-mech-pad",
    "delta-mech-pad", "legacy-mech-pad",

    "draug-factory", "legacy-unit-factory",
    "spirit-factory", "legacy-unit-factory",
    "phantom-factory", "legacy-unit-factory",
    "wraith-factory", "legacy-unit-factory",
    "ghoul-factory", "legacy-unit-factory-air",
    "revenant-factory", "legacy-unit-factory-air",
    "dagger-factory", "legacy-unit-factory",
    "crawler-factory", "legacy-unit-factory",
    "titan-factory", "legacy-unit-factory-ground",
    "fortress-factory", "legacy-unit-factory-ground",

    "mass-conveyor", "payload-conveyor",
    "vestige", "scepter",
    "turbine-generator", "steam-generator",

    "rocks", "stone-wall",
    "sporerocks", "spore-wall",
    "icerocks", "ice-wall",
    "dunerocks", "dune-wall",
    "sandrocks", "sand-wall",
    "shalerocks", "shale-wall",
    "snowrocks", "snow-wall",
    "saltrocks", "salt-wall",
    "dirtwall", "dirt-wall",

    "ignarock", "basalt",
    "holostone", "dacite",
    "holostone-wall", "dacite-wall",
    "rock", "boulder",
    "snowrock", "snow-boulder",
    "cliffs", "stone-wall",
    "craters", "crater-stone",
    "deepwater", "deep-water",
    "water", "shallow-water",
    "sand", "sand-floor",
    "slag", "molten-slag",

    "cryofluidmixer", "cryofluid-mixer",
    "block-forge", "constructor",
    "block-unloader", "payload-unloader",
    "block-loader", "payload-loader",
    "thermal-pump", "impulse-pump",
    "alloy-smelter", "surge-smelter",
    "steam-vent", "rhyolite-vent",
    "fabricator", "tank-fabricator",
    "basic-reconstructor", "refabricator"
    );

    public static final ObjectMap<String, String> modContentNameMap = ObjectMap.of(
    "craters", "crater-stone",
    "deepwater", "deep-water",
    "water", "shallow-water",
    "slag", "molten-slag"
    );

    protected static final ReusableByteOutStream byteOutput = new ReusableByteOutStream(), byteOutput2 = new ReusableByteOutStream();
    protected static final DataOutputStream dataBytes = new DataOutputStream(byteOutput), dataBytes2 = new DataOutputStream(byteOutput2);
    protected static final Writes writes1 = new Writes(dataBytes), writes2 = new Writes(dataBytes2);
    protected static final Reads chunkReads = new Reads(null);
    protected static boolean chunkNested = false;

    public static String mapFallback(String name){
        return fallback.get(name, name);
    }

    //TODO: unify readRegion with readChunk, they do the same thing and both should have good error messages
    public void readRegion(String name, DataInput stream, CounterInputStream counter, IORunner<DataInput> cons) throws IOException{
        counter.resetCount();
        int length;
        try{
            length = readChunk(stream, (chunkStream, len) -> cons.accept(chunkStream));
        }catch(Throwable e){
            throw new IOException("Error reading region \"" + name + "\".", e);
        }

        if(length != counter.count - 4){
            throw new IOException("Error reading region \"" + name + "\": read length mismatch. Expected: " + length + "; Actual: " + (counter.count - 4));
        }
    }

    public void writeRegion(String name, DataOutput stream, IORunner<DataOutput> cons) throws IOException{
        try{
            writeChunk(stream, writes -> cons.accept(writes.output));
        }catch(Throwable e){
            throw new IOException("Error writing region \"" + name + "\".", e);
        }
    }

    /** Write a chunk of input to the stream. An integer of some length is written first, followed by the data. */
    public void writeChunk(DataOutput output, IORunner<Writes> runner) throws IOException{
        boolean wasNested = chunkNested;

        chunkNested = true;

        //regions can be nested once, so use a different output if it's already nested
        ReusableByteOutStream dout = wasNested ? byteOutput2 : byteOutput;

        try{
            //reset output position
            dout.reset();
            //write the needed info
            runner.accept(wasNested ? writes2 : writes1);

            //write the length of the stream contents as an int, then write the contents
            int length = dout.size();
            output.writeInt(length);
            output.write(dout.getBytes(), 0, length);
        }finally{
            chunkNested = wasNested;
        }
    }

    /** Reads a chunk of some length. Use the runner for reading to catch more descriptive errors. */
    public int readChunk(DataInput input, IORunnerLength<DataInput> runner) throws IOException{
        //TODO: it would be really nice to support counting here to detect serialization errors
        int length = input.readInt();
        runner.accept(input, length);
        return length;
    }

    /** Reads a chunk of some length. Use the runner for reading to catch more descriptive errors. */
    public int readChunkReads(DataInput input, IORunnerLength<Reads> runner) throws IOException{
        return readChunk(input, (in, length) -> {
            chunkReads.input = in;
            runner.accept(chunkReads, length);
        });
    }

    /** Skip a chunk completely, discarding the bytes. */
    public void skipChunk(DataInput input) throws IOException{
        int length = readChunk(input, (t, len) -> {});
        int skipped = input.skipBytes(length);
        if(length != skipped){
            throw new IOException("Could not skip bytes. Expected length: " + length + "; Actual length: " + skipped);
        }
    }

    /** Reads a legacy chunk where the length is only 2 bytes. */
    public int readLegacyShortChunk(DataInput input, IORunnerLength<Reads> runner) throws IOException{
        int length = input.readUnsignedShort();
        chunkReads.input = input;
        runner.accept(chunkReads, length);
        return length;
    }

    /** Skip a legacy chunk completely, discarding the bytes. */
    public void skipLegacyShortChunk(DataInput input) throws IOException{
        int length = readLegacyShortChunk(input, (t, len) -> {});
        int skipped = input.skipBytes(length);
        if(length != skipped){
            throw new IOException("Could not skip bytes. Expected length: " + length + "; Actual length: " + skipped);
        }
    }

    public void writeStringMap(DataOutput stream, ObjectMap<String, String> map) throws IOException{
        stream.writeShort(map.size);
        for(Entry<String, String> entry : map.entries()){
            stream.writeUTF(entry.key);
            stream.writeUTF(entry.value);
        }
    }

    public StringMap readStringMap(DataInput stream) throws IOException{
        StringMap map = new StringMap();
        short size = stream.readShort();
        for(int i = 0; i < size; i++){
            map.put(stream.readUTF(), stream.readUTF());
        }
        return map;
    }

    public abstract void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;

    public interface IORunner<T>{
        void accept(T stream) throws IOException;
    }

    public interface IORunnerLength<T>{
        void accept(T stream, int length) throws IOException;
    }

    public interface CustomChunk{
        void write(DataOutput stream) throws IOException;
        void read(DataInput stream) throws IOException;

        default void read(DataInput stream, int length) throws IOException{
            read(stream);
        }

        /** @return whether this chunk is enabled at all */
        default boolean shouldWrite(){
            return true;
        }

        /** @return whether this chunk should be written to connecting clients (default true) */
        default boolean writeNet(){
            return true;
        }
    }
}
