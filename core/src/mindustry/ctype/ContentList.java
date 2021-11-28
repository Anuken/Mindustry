package mindustry.ctype;

/** @deprecated single-method interfaces don't need to exist for content loading; just call YouList.load() directly in the order necessary. */
@Deprecated
public interface ContentList{
    /** This method should create all the content. */
    void load();
}
