package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.ucore.util.Bundles;

public class BlockStats {
    private OrderedMap<String, Object> map = new OrderedMap<>();

    public void add(String label, Object value){
        if(!Bundles.has("text.blocks." + label)) throw new RuntimeException("No bundle entry for description label \"" + label + "\"!");
        if(map.containsKey(label)) throw new RuntimeException("Duplicate label entry: \"" +label + "\"");
        map.put(label, value);
    }

    public void remove(String label){
        Object o =  map.remove(label) != null;
        if(o == null) throw new RuntimeException("No label entry found with name \"" + label + "\"!");
    }

    public OrderedMap<String, Object> getMap() {
        return map;
    }
}
