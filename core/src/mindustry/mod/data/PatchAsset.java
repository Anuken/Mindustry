package mindustry.mod.data;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;

import java.io.*;

public class PatchAsset extends DataAsset{
    private static final JsonValue emptyValue = new JsonValue("error");

    /** Raw string value, containing original formatting. */
    public String patch = "";
    /** Parsed JSON value. Can be an empty error value if parsing failed. */
    public JsonValue json = emptyValue;
    /** True if an error was encountered. */
    public boolean error;
    /** Warnings encountered during patching. */
    public Seq<String> warnings = new Seq<>();

    public PatchAsset(String patch){
        //patches don't have a path by default, so make it something random when reading. TODO: this is a temporary measure.
        setPath("patch-" + Mathf.rand.nextLong() + ".json");
        this.patch = patch;
    }

    PatchAsset(){}

    @Override
    public void readOverride(String path, Fi file) throws IOException{
        setPath(path);
        patch = Jval.read(file.readString()).toString(Jformat.plain);
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.patch;
    }

    @Override
    public void read(DataInput stream) throws IOException{
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        patch = new String(bytes, Strings.utf8);
    }

    @Override
    public void write(DataOutput stream) throws IOException{
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
