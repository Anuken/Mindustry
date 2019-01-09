package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;

/**Stores player unlocks. Clientside only.*/
public class GlobalData{
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private ObjectIntMap<Item> items = new ObjectIntMap<>();

    public GlobalData(){
        Core.settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
        Core.settings.setSerializer(Item.class, (stream, t) -> stream.writeUTF(t.name), stream -> Vars.content.getByName(ContentType.item, stream.readUTF()));
    }

    public ObjectIntMap<Item> items(){
        return items;
    }

    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        return content.alwaysUnlocked() || unlocked.getOr(content.getContentType(), ObjectSet::new).contains(content.getContentName());
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

        boolean ret = unlocked.getOr(content.getContentType(), ObjectSet::new).add(content.getContentName());

        //fire unlock event so other classes can use it
        if(ret){
            content.onUnlock();
            Events.fire(new UnlockEvent(content));
            save();
        }

        return ret;
    }

    /** Clears all unlocked content. Automatically saves.*/
    public void reset(){
        save();
    }

    @SuppressWarnings("unchecked")
    public void load(){
        unlocked = Core.settings.getObject("unlocks", ObjectMap.class, ObjectMap::new);
        items = Core.settings.getObject("items", ObjectIntMap.class, ObjectIntMap::new);
    }

    public void save(){
        Core.settings.putObject("unlocks", unlocked);
        Core.settings.putObject("items", items);
        Core.settings.save();
    }

}
