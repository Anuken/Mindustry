package mindustry.world.blocks;

import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.world.meta.Attribute;

import java.util.*;

public class Attributes implements Serializable{
    private final float[] arr = new float[Attribute.all.length];

    public void clear(){
        Arrays.fill(arr, 0);
    }

    public float get(Attribute attr){
        return arr[attr.ordinal()];
    }

    public void set(Attribute attr, float value){
        arr[attr.ordinal()] = value;
    }

    public void add(Attributes other){
        for(int i = 0; i < arr.length; i++){
            arr[i] += other.arr[i];
        }
    }

    public void add(Attributes other, float scl){
        for(int i = 0; i < arr.length; i++){
            arr[i] += other.arr[i] * scl;
        }
    }

    @Override
    public void write(Json json){
        for(Attribute at : Attribute.all){
            if(arr[at.ordinal()] != 0){
                json.writeValue(at.name(), arr[at.ordinal()]);
            }
        }
    }

    @Override
    public void read(Json json, JsonValue data){
        for(Attribute at : Attribute.all){
            arr[at.ordinal()] = data.getFloat(at.name(), 0);
        }
    }
}
