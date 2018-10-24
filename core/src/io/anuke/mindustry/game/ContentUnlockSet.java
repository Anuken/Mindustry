package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.ucore.core.Events;

public class ContentUnlockSet {
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private boolean dirty;

    public boolean isUnlocked(UnlockableContent content){
        if(content.alwaysUnlocked()) return true;

        if(!unlocked.containsKey(content.getContentType())){
            unlocked.put(content.getContentType(), new ObjectSet<>());
        }

        ObjectSet<String> set = unlocked.get(content.getContentType());

        return set.contains(content.getContentName());
    }

    public boolean unlockContent(UnlockableContent content){
        if(!content.canBeUnlocked() || content.alwaysUnlocked()) return false;

        if(!unlocked.containsKey(content.getContentType())){
            unlocked.put(content.getContentType(), new ObjectSet<>());
        }

        boolean ret = unlocked.get(content.getContentType()).add(content.getContentName());

        //fire unlock event so other classes can use it
        if(ret){
            content.onUnlock();
            Events.fire(new UnlockEvent(content));
            dirty = true;
        }

        return ret;
    }

    public boolean isDirty() {
        return dirty;
    }

    public ObjectMap<ContentType, ObjectSet<String>> getUnlocked() {
        return unlocked;
    }
}
