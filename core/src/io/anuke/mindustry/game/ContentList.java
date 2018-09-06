package io.anuke.mindustry.game;

import io.anuke.mindustry.type.ContentType;

/**Interface for a list of content to be loaded in {@link io.anuke.mindustry.core.ContentLoader}.*/
public interface ContentList{
    /**This method should create all the content.*/
    void load();

    /**This method should return the type of content being loaded.*/
    ContentType type();
}
