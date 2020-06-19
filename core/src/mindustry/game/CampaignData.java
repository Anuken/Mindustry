package mindustry.game;

import arc.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;

/** Stores unlocks and tech tree state. */
public class CampaignData{
    private ObjectSet<String> unlocked = new ObjectSet<>();

    /** @return whether or not this piece of content is unlocked yet. */
    public boolean isUnlocked(UnlockableContent content){
        return content.alwaysUnlocked || unlocked.contains(content.name);
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked, nothing changes.
     */
    public void unlockContent(UnlockableContent content){
        if(content.alwaysUnlocked) return;

        //fire unlock event so other classes can use it
        if(unlocked.add(content.name)){
            content.onUnlock();
            Events.fire(new UnlockEvent(content));

            save();
        }
    }

    /** Clears all unlocked content. Automatically saves. */
    public void reset(){
        save();
    }

    @SuppressWarnings("unchecked")
    public void load(){
        unlocked = Core.settings.getJson("unlocked-content", ObjectSet.class, ObjectSet::new);
    }

    public void save(){
        Core.settings.putJson("unlocked-content", String.class, unlocked);
    }

}
