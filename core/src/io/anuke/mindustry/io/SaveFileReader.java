package io.anuke.mindustry.io;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.collection.StringMap;
import io.anuke.arc.util.io.CounterInputStream;
import io.anuke.arc.util.io.ReusableByteOutStream;
import io.anuke.mindustry.world.WorldContext;

import java.io.*;

public abstract class SaveFileReader{
    protected final ReusableByteOutStream byteOutput = new ReusableByteOutStream();
    protected final DataOutputStream dataBytes = new DataOutputStream(byteOutput);
    protected final ReusableByteOutStream byteOutputSmall = new ReusableByteOutStream();
    protected final DataOutputStream dataBytesSmall = new DataOutputStream(byteOutputSmall);
    protected final ObjectMap<String, String> fallback = ObjectMap.of();

    protected void region(String name, DataInput stream, CounterInputStream counter, IORunner<DataInput> cons) throws IOException{
        counter.resetCount();
        int length;
        try{
            length = readChunk(stream, cons);
        }catch(Throwable e){
            throw new IOException("Error reading region \"" + name + "\".", e);
        }

        if(length != counter.count() - 4){
            throw new IOException("Error reading region \"" + name + "\": read length mismatch. Expected: " + length + "; Actual: " + (counter.count() - 4));
        }
    }

    protected void region(String name, DataOutput stream, IORunner<DataOutput> cons) throws IOException{
        try{
            writeChunk(stream, cons);
        }catch(Throwable e){
            throw new IOException("Error writing region \"" + name + "\".", e);
        }
    }

    public void writeChunk(DataOutput output, IORunner<DataOutput> runner) throws IOException{
        writeChunk(output, false, runner);
    }

    /** Write a chunk of input to the stream. An integer of some length is written first, followed by the data. */
    public void writeChunk(DataOutput output, boolean isByte, IORunner<DataOutput> runner) throws IOException{
        ReusableByteOutStream dout = isByte ? byteOutputSmall : byteOutput;
        //reset output position
        dout.reset();
        //write the needed info
        runner.accept(isByte ? dataBytesSmall : dataBytes);
        int length = dout.size();
        //write length (either int or byte) followed by the output bytes
        if(!isByte){
            output.writeInt(length);
        }else{
            if(length > Short.MAX_VALUE){
                throw new IOException("Byte write length exceeded: " + length + " > " + Short.MAX_VALUE);
            }
            output.writeShort(length);
        }
        output.write(dout.getBytes(), 0, length);
    }

    public int readChunk(DataInput input, IORunner<DataInput> runner) throws IOException{
        return readChunk(input, false, runner);
    }

    /** Reads a chunk of some length. Use the runner for reading to catch more descriptive errors. */
    public int readChunk(DataInput input, boolean isByte, IORunner<DataInput> runner) throws IOException{
        int length = isByte ? input.readUnsignedShort() : input.readInt();
        runner.accept(input);
        return length;
    }

    public void skipRegion(DataInput input) throws IOException{
        skipRegion(input, false);
    }

    /** Skip a region completely. */
    public void skipRegion(DataInput input, boolean isByte) throws IOException{
        int length = readChunk(input, isByte, t -> {});
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

    protected interface IORunner<T>{
        void accept(T stream) throws IOException;
    }
}
