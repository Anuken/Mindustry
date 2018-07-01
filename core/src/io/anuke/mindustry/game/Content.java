package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;

/**Base interface for a content type that is loaded in {@link io.anuke.mindustry.core.ContentLoader}.*/
public interface Content {

    /**Returns the type name of this piece of content.
     * This should return the same value for all instances of this content type.*/
    String getContentTypeName();

    /**Returns a list of all instances of this content.*/
    Array<? extends Content> getAll();

    /**Called after all content is created. Do not use to load regions or texture data!*/
    default void init(){}

    /**Called after all content is created, only on non-headless versions.
     * Use for loading regions or other image data.*/
    default void load(){}
}
