package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ContentType;
import io.anuke.ucore.core.Settings;

import static io.anuke.mindustry.Vars.*;

/**Stores player unlocks. Clientside only.*/
public class Unlocks{
    private ObjectMap<String, ContentUnlockSet> sets = new ObjectMap<>();

    static{
        Settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
    }

    /**Handles the event of content being used by either the player or some block.*/
    public void handleContentUsed(UnlockableContent content){
        if(world.getSector() != null){
            world.getSector().currentMission().onContentUsed(content);
        }
        unlockContent(content);
    }
    
    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        return rootSet().isUnlocked(content) || currentSet().isUnlocked(content);
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked or cannot be unlocked due to dependencies, nothing changes.
     * Results are not saved until you call {@link #save()}.
     *
     * @return whether or not this content was newly unlocked.
     */
    public boolean unlockContent(UnlockableContent content){
        return !rootSet().isUnlocked(content) && currentSet().unlockContent(content);
    }

    private ContentUnlockSet currentSet(){
        //client connected to server: always return the IP-specific set
        if(Net.client()){
            return getSet(Net.getLastIP());
        }else if((world.getSector() != null || state.mode.infiniteResources) || state.is(State.menu)){ //sector-sandbox have shared set
            return rootSet();
        }else{ //per-mode set
            return getSet(state.mode.name());
        }
    }

    private ContentUnlockSet rootSet(){
        return getSet("root");
    }

    private ContentUnlockSet getSet(String name){
        if(!sets.containsKey(name)){
            sets.put(name, new ContentUnlockSet());
        }
        return sets.get(name);
    }

    /** Returns whether unlockables have changed since the last save.*/
    public boolean isDirty(){
        for(ContentUnlockSet set : sets.values()){
            if(set.isDirty()){
                return true;
            }
        }
        return false;
    }

    /** Clears all unlocked content. Automatically saves.*/
    public void reset(){
        sets.clear();
        save();
    }

    public void load(){
        sets.clear();

        ObjectMap<String, ObjectMap<ContentType, Array<String>>> result = Settings.getObject("content-sets", ObjectMap.class, ObjectMap::new);

        for(Entry<String, ObjectMap<ContentType, Array<String>>> outer : result.entries()){
            ContentUnlockSet cset = new ContentUnlockSet();
            for (Entry<ContentType, Array<String>> entry : outer.value.entries()){
                ObjectSet<String> set = new ObjectSet<>();
                set.addAll(entry.value);
                cset.getUnlocked().put(entry.key, set);
            }
            sets.put(outer.key, cset);
        }
    }

    public void save(){
        ObjectMap<String, ObjectMap<ContentType, Array<String>>> output = new ObjectMap<>();

        for(Entry<String, ContentUnlockSet> centry : sets.entries()){
            ObjectMap<ContentType, Array<String>> write = new ObjectMap<>();

            for(Entry<ContentType, ObjectSet<String>> entry : centry.value.getUnlocked().entries()){
                write.put(entry.key, entry.value.iterator().toArray());
            }

            output.put(centry.key, write);
        }

        Settings.putObject("content-sets", output);
        Settings.save();
    }

}
