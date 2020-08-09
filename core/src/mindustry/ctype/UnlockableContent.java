package mindustry.ctype;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/** Base interface for an unlockable content type. */
public abstract class UnlockableContent extends MappableContent{
    /** Localized, formal name. Never null. Set to internal name if not found in bundle. */
    public String localizedName;
    /** Localized description. May be null. */
    public @Nullable String description;
    /** Whether this content is always unlocked in the tech tree. */
    public boolean alwaysUnlocked = false;
    /** Icons by Cicon ID.*/
    protected TextureRegion[] cicons = new TextureRegion[mindustry.ui.Cicon.all.length];
    /** Unlock state. Loaded from settings. Do not modify outside of the constructor. */
    protected boolean unlocked;

    public UnlockableContent(String name){
        super(name);

        this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
        this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
        this.unlocked = Core.settings != null && Core.settings.getBool(name + "-unlocked", false);
    }

    public String displayDescription(){
        return minfo.mod == null ? description : description + "\n" + Core.bundle.format("mod.display", minfo.mod.meta.displayName());
    }

    /** Generate any special icons for this content. Called asynchronously.*/
    @CallSuper
    public void createIcons(MultiPacker packer){

    }

    public String emoji(){
        return Fonts.getUnicodeStr(name);
    }

    /** Returns a specific content icon, or the region {contentType}-{name} if not found.*/
    public TextureRegion icon(Cicon icon){
        if(cicons[icon.ordinal()] == null){
            cicons[icon.ordinal()] =
                Core.atlas.find(getContentType().name() + "-" + name + "-" + icon.name(),
                Core.atlas.find(getContentType().name() + "-" + name + "-full",
                Core.atlas.find(name,
                Core.atlas.find(getContentType().name() + "-" + name,
                Core.atlas.find(name + "1")))));
        }
        return cicons[icon.ordinal()];
    }

    /** Iterates through any implicit dependencies of this content.
     * For blocks, this would be the items required to build it. */
    public void getDependencies(Cons<UnlockableContent> cons){

    }

    /** This should show all necessary info about this content in the specified table. */
    public abstract void displayInfo(Table table);

    /** Called when this content is unlocked. Use this to unlock other related content. */
    public void onUnlock(){
    }

    /** Whether this content is always hidden in the content database dialog. */
    public boolean isHidden(){
        return false;
    }

    /** Makes this piece of content unlocked; if it already unlocked, nothing happens. */
    public void unlock(){
        if(!unlocked()){
            unlocked = true;
            Core.settings.put(name + "-unlocked", true);

            onUnlock();
            Events.fire(new UnlockEvent(this));
        }
    }

    public final boolean unlocked(){
        return unlocked || alwaysUnlocked;
    }

    /** @return whether this content is unlocked, or the player is in a custom (non-campaign) game. */
    public final boolean unlockedNow(){
        return unlocked || alwaysUnlocked || !state.isCampaign();
    }

    public final boolean locked(){
        return !unlocked();
    }
}
