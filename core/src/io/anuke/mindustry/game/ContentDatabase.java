package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

public class ContentDatabase {
    private ObjectMap<String, ObjectSet<String>> unlocked = new ObjectMap<>();

    public boolean isUnlocked(Content content){
        if(!unlocked.containsKey(content.getContentTypeName())){
            unlocked.put(content.getContentTypeName(), new ObjectSet<>());
        }

        ObjectSet<String> set = unlocked.get(content.getContentTypeName());

        return set.contains(content.getContentName());
    }

    private void load(){

    }

    private void save(){

    }

}
