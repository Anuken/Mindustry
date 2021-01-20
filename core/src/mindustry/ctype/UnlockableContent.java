package mindustry.ctype;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Base interface for an unlockable content type. */
public abstract class UnlockableContent extends MappableContent{
    /** Stat storage for this content. Initialized on demand. */
    public Stats stats = new Stats();
    /** Localized, formal name. Never null. Set to internal name if not found in bundle. */
    public String localizedName;
    /** Localized description & details. May be null. */
    public @Nullable String description, details;
    /** Whether this content is always unlocked in the tech tree. */
    public boolean alwaysUnlocked = false;
    /** Whether to show the description in the research dialog preview. */
    public boolean inlineDescription = true;
    /** Special logic icon ID. */
    public int iconId = 0;
    /** Icons by Cicon ID.*/
    protected TextureRegion[] cicons = new TextureRegion[Cicon.all.length];
    /** Unlock state. Loaded from settings. Do not modify outside of the constructor. */
    protected boolean unlocked;

    public UnlockableContent(String name){
        super(name);

        this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
        this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
        this.details = Core.bundle.getOrNull(getContentType() + "." + this.name + ".details");
        this.unlocked = Core.settings != null && Core.settings.getBool(this.name + "-unlocked", false);
    }

    /** @return the tech node for this content. may be null. */
    public @Nullable TechNode node(){
        return TechTree.get(this);
    }

    public String displayDescription(){
        return minfo.mod == null ? description : description + "\n" + Core.bundle.format("mod.display", minfo.mod.meta.displayName());
    }

    /** Checks stat initialization state. Call before displaying stats. */
    public void checkStats(){
        if(!stats.intialized){
            setStats();
            stats.intialized = true;
        }
    }

    /** Initializes stats on demand. Should only be called once. Only called before something is displayed. */
    public void setStats(){
    }

    /** Generate any special icons for this content. Called asynchronously.*/
    @CallSuper
    public void createIcons(MultiPacker packer){

    }

    /** @return items needed to research this content */
    public ItemStack[] researchRequirements(){
        return ItemStack.empty;
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
                Core.atlas.find(name + "-" + icon.name(),
                Core.atlas.find(name + "-full",
                Core.atlas.find(name,
                Core.atlas.find(getContentType().name() + "-" + name,
                Core.atlas.find(name + "1")))))));
        }
        return cicons[icon.ordinal()];
    }

    /** Iterates through any implicit dependencies of this content.
     * For blocks, this would be the items required to build it. */
    public void getDependencies(Cons<UnlockableContent> cons){

    }

    /** This should show all necessary info about this content in the specified table. */
    public void display(Table table){

    }

    /** Called when this content is unlocked. Use this to unlock other related content. */
    public void onUnlock(){
    }

    /** Whether this content is always hidden in the content database dialog. */
    public boolean isHidden(){
        return false;
    }

    /** Makes this piece of content unlocked; if it already unlocked, nothing happens. */
    public void unlock(){
        if(!unlocked && !alwaysUnlocked){
            unlocked = true;
            Core.settings.put(name + "-unlocked", true);

            onUnlock();
            Events.fire(new UnlockEvent(this));
        }
    }

    /** Unlocks this content, but does not fire any events. */
    public void quietUnlock(){
        if(!unlocked()){
            unlocked = true;
            Core.settings.put(name + "-unlocked", true);
        }
    }

    public boolean unlocked(){
        if(net != null && net.client()) return unlocked || alwaysUnlocked || state.rules.researched.contains(name);
        return unlocked || alwaysUnlocked;
    }

    /** Locks this content again. */
    public void clearUnlock(){
        if(unlocked){
            unlocked = false;
            Core.settings.put(name + "-unlocked", false);
        }
    }

    /** @return whether this content is unlocked, or the player is in a custom (non-campaign) game. */
    public boolean unlockedNow(){
        return unlocked() || !state.isCampaign();
    }

    public boolean locked(){
        return !unlocked();
    }
}
