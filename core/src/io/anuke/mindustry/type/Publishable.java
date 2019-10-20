package io.anuke.mindustry.type;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;

/** Defines a piece of content that can be published on the Workshop. */
public interface Publishable{
    /** @return workshop item ID, or null if this isn't on the workshop. */
    @Nullable String getSteamID();
    /** adds a steam ID to this item once it's published. should save the item to make sure this change is persisted. */
    void addSteamID(String id);
    /** removes the item ID; called when the item isn't found. */
    void removeSteamID();
    /** @return default title of the listing. */
    String steamTitle();
    /** @return standard steam listing description, may be null. this is editable by users after release.*/
    @Nullable String steamDescription();
    /** @return the tag that this content has. e.g. 'schematic' or 'map'. */
    String steamTag();
    /** @return a folder with everything needed for this piece of content in it; does not need to be a copy. */
    FileHandle createSteamFolder(String id);
    /** @return a preview file PNG. */
    FileHandle createSteamPreview(String id);
    /** @return any extra tags to add to this item.*/
    default Array<String> extraTags(){
        return new Array<>(0);
    }
    /** @return whether this item is or was once on the workshop.*/
    default boolean hasSteamID(){
        return getSteamID() != null && Vars.steam;
    }
    /** called before this item is published.
     * @return true to signify that everything is cool and good, or false to significy that the user has done something wrong.
     * if false is returned, make sure to show a dialog explaining the error. */
    default boolean prePublish(){
        return true;
    }
}
