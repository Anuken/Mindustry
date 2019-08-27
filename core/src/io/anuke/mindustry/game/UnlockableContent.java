package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.Vars;

/** Base interface for an unlockable content type. */
public abstract class UnlockableContent extends MappableContent{
    /** Localized, formal name. Never null. Set to block name if not found in bundle. */
    public String localizedName;
    /** Localized description. May be null. */
    public String description;

    public UnlockableContent(String name){
        super(name);

        this.localizedName = Core.bundle.get(getContentType() + "." + name + ".name", name);
        this.description = Core.bundle.getOrNull(getContentType() + "." + name + ".description");
    }

    /** Returns the localized name of this content. */
    public abstract String localizedName();

    public abstract TextureRegion getContentIcon();

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

    public final boolean locked(){
        return !unlocked();
    }
}
