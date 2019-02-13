package io.anuke.mindustry.game;

public abstract class MappableContent extends Content {
    /**
     * Returns the unqiue name of this piece of content.
     * The name only needs to be unique for all content of this type.
     * Do not use IDs for names! Make sure this string stays constant with each update unless removed.
     */
    public abstract String getContentName();

    @Override
    public String toString(){
        return getContentName();
    }
}
