package mindustry.ctype;

import arc.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.ui.Cicon;

/** Base interface for an unlockable content type. */
public abstract class UnlockableContent extends MappableContent{
    /** Localized, formal name. Never null. Set to block name if not found in bundle. */
    public String localizedName;
    /** Localized description. May be null. */
    public @Nullable String description;
    /** Icons by Cicon ID.*/
    protected TextureRegion[] cicons = new TextureRegion[mindustry.ui.Cicon.all.length];

    public UnlockableContent(String name){
        super(name);

        this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
        this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
    }

    public String displayDescription(){
        return minfo.mod == null ? description : description + "\n" + Core.bundle.format("mod.display", minfo.mod.meta.displayName());
    }

    /** Generate any special icons for this content. Called asynchronously.*/
    @CallSuper
    public void createIcons(MultiPacker packer){

    }

    /** Returns a specific content icon, or the region {contentType}-{name} if not found.*/
    public TextureRegion icon(Cicon icon){
        if(cicons[icon.ordinal()] == null){
            cicons[icon.ordinal()] = Core.atlas.find(getContentType().name() + "-" + name + "-" + icon.name(),
                Core.atlas.find(getContentType().name() + "-" + name + "-full",
                Core.atlas.find(getContentType().name() + "-" + name,
                Core.atlas.find(name,
                Core.atlas.find(name + "1")))));
        }
        return cicons[icon.ordinal()];
    }

    /** This should show all necessary info about this content in the specified table. */
    public abstract void displayInfo(Table table);

    /** Called when this content is unlocked. Use this to unlock other related content. */
    public void onUnlock(){
    }

    /** Whether this content is always hidden in the content info dialog. */
    public boolean isHidden(){
        return false;
    }

    /** Override to make content always unlocked. */
    public boolean alwaysUnlocked(){
        return false;
    }

    public final boolean unlocked(){
        return Vars.data.isUnlocked(this);
    }

    /** @return whether this content is unlocked, or the player is in a custom game. */
    public final boolean unlockedCur(){
        return Vars.data.isUnlocked(this) || !Vars.world.isZone();
    }

    public final boolean locked(){
        return !unlocked();
    }
}
