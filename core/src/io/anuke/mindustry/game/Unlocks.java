package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.arc.Events;
import io.anuke.arc.Settings;

/**Stores player unlocks. Clientside only.*/
public class Unlocks{
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private boolean dirty;

    public Unlocks(){
        Core.settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
    }

    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        if(content.alwaysUnlocked()) return true;

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

    /** Returns whether unlockables have changed since the last save.*/
    public boolean isDirty(){
        return dirty;
    }

    /** Clears all unlocked content. Automatically saves.*/
    public void reset(){
        save();
    }

    @SuppressWarnings("unchecked")
    public void load(){
        unlocked = Core.settings.getObject("unlockset", ObjectMap.class, ObjectMap::new);
    }

    public void save(){
        Core.settings.putObject("unlockset", unlocked);
        Core.settings.save();
    }

}
