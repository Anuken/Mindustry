package mindustry.type;

import arc.struct.*;
import arc.files.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;

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
    Fi createSteamFolder(String id);
    /** @return a preview file PNG. */
    Fi createSteamPreview(String id);
    /** @return any extra tags to add to this item.*/
    default Seq<String> extraTags(){
        return new Seq<>(0);
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
