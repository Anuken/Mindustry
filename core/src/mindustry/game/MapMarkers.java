package mindustry.game;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.MapObjectives.*;
import mindustry.io.*;

import java.io.*;

public class MapMarkers{
    /** Maps marker unique ID to marker. */
    private IntMap<ObjectiveMarker> map = new IntMap<>();

    public Seq<ObjectiveMarker> worldMarkers = new Seq<>(false);
    public Seq<ObjectiveMarker> mapMarkers = new Seq<>(false);
    public Seq<ObjectiveMarker> lightMarkers = new Seq<>(false);

    public void add(int id, ObjectiveMarker marker){
        if(marker == null) return;
        var prev = map.put(id, marker);

        setMarker(worldMarkers, marker, prev, m -> m.world, (m, i) -> m.world = i);
        setMarker(mapMarkers, marker, prev, m -> m.minimap, (m, i) -> m.minimap = i);
        setMarker(lightMarkers, marker, prev, m -> m.light, (m, i) -> m.light = i);
    }

    public void remove(int id){
        var prev = map.remove(id);

        if(prev != null){
            remove(worldMarkers, prev.world, (m, i) -> m.world = i);
            remove(mapMarkers, prev.minimap, (m, i) -> m.minimap = i);
            remove(lightMarkers, prev.light, (m, i) -> m.light = i);
        }
    }

    public @Nullable ObjectiveMarker get(int id){
        return map.get(id);
    }

    public boolean has(int id){
        return get(id) != null;
    }

    public int size(){
        return map.size;
    }

    public void write(DataOutput stream) throws IOException{
        JsonIO.writeBytes(map, ObjectiveMarker.class, (DataOutputStream)stream);
    }

    public void read(DataInput stream) throws IOException{
        worldMarkers.clear();
        mapMarkers.clear();
        lightMarkers.clear();
        map = JsonIO.readBytes(IntMap.class, ObjectiveMarker.class, (DataInputStream)stream);

        for(var entry : map.entries()){
            var marker = entry.value;

            if(marker.world != -1) marker.world = worldMarkers.add(marker).size - 1;
            if(marker.minimap != -1) marker.minimap = mapMarkers.add(marker).size - 1;
            if(marker.light != -1) marker.light = lightMarkers.add(marker).size - 1;
        }
    }

    public interface MarkerSetter{
        void set(ObjectiveMarker marker, int index);
    }

    public void updateMarker(Seq<ObjectiveMarker> markers, ObjectiveMarker marker, boolean visible, Intf<ObjectiveMarker> getter, MarkerSetter setter){
        if((getter.get(marker) != -1) == visible) return; // nothing to change

        if(!visible){
            setter.set(markers.peek(), getter.get(marker));
            markers.remove(getter.get(marker));
            setter.set(marker, -1);
        }else{
            setter.set(marker, markers.size);
            markers.add(marker);
        }
    }

    private void setMarker(Seq<ObjectiveMarker> markers, ObjectiveMarker curr, ObjectiveMarker prev, Intf<ObjectiveMarker> getter, MarkerSetter setter){
        int currIndex = getter.get(curr);

        if(prev != null && getter.get(prev) != -1){
            int prevIndex = getter.get(prev);
            if(currIndex != -1){
                // both markers visible, replace previous with current
                setter.set(curr, prevIndex);
                markers.set(prevIndex, curr);
            }else{
                // previous marker visible but not current
                setter.set(markers.peek(), prevIndex);
                markers.remove(prevIndex);
            }
        }else{
            if(currIndex != -1){
                setter.set(curr, markers.size);
                markers.add(curr);
            }
        }
    }

    private void remove(Seq<ObjectiveMarker> markers, int index, MarkerSetter setter){
        if(index != -1){
            setter.set(markers.peek(), index);
            markers.remove(index);
        }
    }

}
