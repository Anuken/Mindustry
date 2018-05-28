package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.ucore.core.Settings;

import static io.anuke.mindustry.Vars.debug;

public class ContentDatabase {
    private ObjectMap<String, ObjectSet<String>> unlocked = new ObjectMap<>();

    public boolean isUnlocked(Content content){
        if(debug){
            return true;
        }

        if(!unlocked.containsKey(content.getContentTypeName())){
            unlocked.put(content.getContentTypeName(), new ObjectSet<>());
        }

        ObjectSet<String> set = unlocked.get(content.getContentTypeName());

        return set.contains(content.getContentName());
    }

    private void load(){
        ObjectMap<String, Array<String>> result = Settings.getJson("content-database", ObjectMap.class);

        for(Entry<String, Array<String>> entry : result.entries()){
            ObjectSet<String> set = new ObjectSet<>();
            set.addAll(entry.value);
            unlocked.put(entry.key, set);
        }
    }

    private void save(){
        ObjectMap<String, Array<String>> write = new ObjectMap<>();

        for(Entry<String, ObjectSet<String>> entry : unlocked.entries()){
            write.put(entry.key, entry.value.iterator().toArray());
        }

        Settings.putJson("content-database", write);
    }

}
