package mindustry.game;

import arc.struct.*;
import arc.util.*;
import mindustry.game.MapObjectives.*;
import mindustry.io.*;

import java.io.*;
import java.util.*;

public class MapMarkers implements Iterable<ObjectiveMarker>{
    /** Maps marker unique ID to marker. */
    private IntMap<ObjectiveMarker> map = new IntMap<>();
    /** Sequential list of markers. This allows for faster iteration than a map. */
    private Seq<ObjectiveMarker> all = new Seq<>(false);

    public void add(int id, ObjectiveMarker marker){
        if(marker == null) return;

        var prev = map.put(id, marker);
        if(prev != null){
            all.set(prev.arrayIndex, marker);
            marker.arrayIndex = prev.arrayIndex;
        }else{
            all.add(marker);
            marker.arrayIndex = all.size - 1;
        }
    }

    public void remove(int id){
        var prev = map.remove(id);
        if(prev != null){
            if(all.size > prev.arrayIndex + 1){ //there needs to be something above the index to replace it with
                all.remove(prev.arrayIndex);
                //update its index
                all.get(prev.arrayIndex).arrayIndex = prev.arrayIndex;
            }else{
                //no sense updating the index of the replaced element when it was not replaced
                all.remove(prev.arrayIndex);
            }
        }
    }

    public @Nullable ObjectiveMarker get(int id){
        return map.get(id);
    }

    public boolean has(int id){
        return get(id) != null;
    }

    public int size(){
        return all.size;
    }

    public void write(DataOutput stream) throws IOException{
        JsonIO.writeBytes(map, ObjectiveMarker.class, (DataOutputStream)stream);
    }

    public void read(DataInput stream) throws IOException{
        all.clear();
        map = JsonIO.readBytes(IntMap.class, ObjectiveMarker.class, (DataInputStream)stream);
        for(var entry : map.entries()){
            all.add(entry.value);
            entry.value.arrayIndex = all.size - 1;
        }
    }

    @Override
    public Iterator<ObjectiveMarker> iterator(){
        return all.iterator();
    }
}
