package mindustry.ctype;

/** Interface for a list of content to be loaded in {@link mindustry.core.ContentLoader}. */
public interface ContentList{
    /** This method should create all the content. */
    void load();
}
