package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;

/**Interface for a list of content to be loaded in {@link io.anuke.mindustry.core.ContentLoader}.*/
public interface ContentList{
    /**This method should create all the content.*/
    void load();

    /**This method should return the list of the content of this type, for further loading.*/
    Array<? extends Content> getAll();
}
