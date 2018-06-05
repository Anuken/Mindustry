package io.anuke.mindustry.game;

/**Base interface for an unlockable content type.*/
public interface UnlockableContent extends Content{

    /**Returns the unqiue name of this piece of content.
     * The name only needs to be unique for all content of this type.
     * Do not use IDs for names! Make sure this string stays constant with each update unless removed.
     * (e.g. having a recipe and a block, both with name "wall" is fine, as they are different types).*/
    String getContentName();
}
