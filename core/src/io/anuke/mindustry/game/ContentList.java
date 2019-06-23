package io.anuke.mindustry.game;

/** Interface for a list of content to be loaded in {@link io.anuke.mindustry.core.ContentLoader}. */
public interface ContentList{
    /** This method should create all the content. */
    void load();
}
