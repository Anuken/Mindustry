package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Settings;

import static io.anuke.mindustry.Vars.debug;

public class ContentDatabase{
    /** Maps unlockable type names to a set of unlocked content.*/
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    /** Whether unlockables have changed since the last save.*/
    private boolean dirty;

    static{
        Settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
    }
    
    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        if(debug) return true;

        if(!unlocked.containsKey(content.getContentType())){
            unlocked.put(content.getContentType(), new ObjectSet<>());
        }

        ObjectSet<String> set = unlocked.get(content.getContentType());

        return set.contains(content.getContentName());
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked or cannot be unlocked due to dependencies, nothing changes.
     * Results are not saved until you call {@link #save()}.
     *
     * @return whether or not this content was newly unlocked.
     */
    public boolean unlockContent(UnlockableContent content){
        if(!content.canBeUnlocked()) return false;

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

    /** Returns whether unlockables have changed since the last save.*/
    public boolean isDirty(){
        return dirty;
    }

    /** Clears all unlocked content.*/
    public void reset(){
        unlocked.clear();
        dirty = true;
    }

    public void load(){
        ObjectMap<ContentType, Array<String>> result = Settings.getBinary("content-database", ObjectMap.class, () -> new ObjectMap<>());

        for(Entry<ContentType, Array<String>> entry : result.entries()){
            ObjectSet<String> set = new ObjectSet<>();
            set.addAll(entry.value);
            unlocked.put(entry.key, set);
        }

        dirty = false;
    }

    public void save(){

        ObjectMap<ContentType, Array<String>> write = new ObjectMap<>();

        for(Entry<ContentType, ObjectSet<String>> entry : unlocked.entries()){
            write.put(entry.key, entry.value.iterator().toArray());
        }

        Settings.putBinary("content-database", write);
        Settings.save();
        dirty = false;
    }

}
