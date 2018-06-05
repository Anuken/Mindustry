package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;

/**Base interface for an unlockable content type.*/
public interface Content {

    /**Returns the type name of this piece of content.
     * This should return the same value for all instances of this content type.*/
    String getContentTypeName();

    /**Returns a list of all instances of this content.*/
    Array<? extends Content> getAll();

    /**Called after all content is created. Use for loading texture regions and other data.
     * Do not use to load regions!*/
    default void init(){}

    /**Called after all content is created, only on non-headless versions.
     * Use for loading regions or other image data.*/
    default void load(){}
}
