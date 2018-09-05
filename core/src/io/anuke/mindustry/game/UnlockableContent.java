package io.anuke.mindustry.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.control;

/**Base interface for an unlockable content type.*/
public interface UnlockableContent extends MappableContent{
    /**Returns the localized name of this content.*/
    String localizedName();

    TextureRegion getContentIcon();

    /**This should show all necessary info about this content in the specified table.*/
    void displayInfo(Table table);

    /**Called when this content is unlocked. Use this to unlock other related content.*/
    default void onUnlock(){
    }

    /**Whether this content is always hidden in the content info dialog.*/
    default boolean isHidden(){
        return false;
    }

    /**Lists the content that must be unlocked in order for this specific content to become unlocked. May return null.*/
    default UnlockableContent[] getDependencies(){
        return null;
    }

    /**Returns whether dependencies are satisfied for unlocking this content.*/
    default boolean canBeUnlocked(){
        UnlockableContent[] depend = getDependencies();
        if(depend == null){
            return true;
        }else{
            for(UnlockableContent cont : depend){
                if(!control.database().isUnlocked(cont)){
                    return false;
                }
            }
            return true;
        }
    }
}
