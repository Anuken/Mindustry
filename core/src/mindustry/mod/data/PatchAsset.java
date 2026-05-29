package mindustry.mod.data;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;

import java.io.*;

public class PatchAsset extends DataAsset{
    private static final JsonValue emptyValue = new JsonValue("error");

    /** Raw string value, containing original formatting. */
    public String patch = "";
    /** Parsed JSON value. Can be an empty error value if parsing failed. */
    public JsonValue json = emptyValue;
    /** Named obtained from patch. */
    public String name = "";
    /** True if an error was encountered. */
    public boolean error;
    /** Warnings encountered during patching. */
    public Seq<String> warnings = new Seq<>();

    public PatchAsset(String patch){
        this.patch = patch;
    }

    PatchAsset(){}

    @Override
    public DataAssetType getType(){
        return DataAssetType.patch;
    }

    @Override
    void read(DataInput stream) throws IOException{
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        patch = new String(bytes, Strings.utf8);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        byte[] bytes = patch.getBytes(Strings.utf8);
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }

    @Override
    public String toString(){
        //the json can be a single 'error' value if it failed to parse
        return !json.isObject() ? patch : json.prettyPrint(OutputType.minimal, 2);
    }
}
