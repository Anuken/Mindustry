package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.type.ContentType;
import io.anuke.ucore.core.Settings;

/**Stores player unlocks. Clientside only.*/
public class Unlocks{
    ContentUnlockSet set = new ContentUnlockSet();

    static{
        Settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
    }

    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        return set.isUnlocked(content);
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked or cannot be unlocked due to dependencies, nothing changes.
     * Results are not saved until you call {@link #save()}.
     *
     * @return whether or not this content was newly unlocked.
     */
    public boolean unlockContent(UnlockableContent content){
        return !set.isUnlocked(content) && currentSet().unlockContent(content);
    }

    private ContentUnlockSet currentSet(){
        return set;
    }

    /** Returns whether unlockables have changed since the last save.*/
    public boolean isDirty(){
        return set.isDirty();
    }

    /** Clears all unlocked content. Automatically saves.*/
    public void reset(){
        save();
    }

    public void load(){
        ObjectMap<ContentType, Array<String>> outer = Settings.getObject("unlocks", ObjectMap.class, ObjectMap::new);
        ContentUnlockSet cset = new ContentUnlockSet();

        for (Entry<ContentType, Array<String>> entry : outer.entries()){
            ObjectSet<String> set = new ObjectSet<>();
            set.addAll(entry.value);
            cset.getUnlocked().put(entry.key, set);
        }
    }

    public void save(){
        ObjectMap<ContentType, Array<String>> write = new ObjectMap<>();

        for(Entry<ContentType, ObjectSet<String>> entry : set.getUnlocked().entries()){
            write.put(entry.key, entry.value.iterator().toArray());
        }

        Settings.putObject("unlocks", write);
        Settings.save();
    }

}
