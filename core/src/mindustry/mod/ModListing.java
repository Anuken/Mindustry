package mindustry.mod;

/** Mod listing as a data class. */
public class ModListing{
    public String repo, name, author, lastUpdated, description;
    public int stars;

    @Override
    public String toString(){
        return "ModListing{" +
        "repo='" + repo + '\'' +
        ", name='" + name + '\'' +
        ", author='" + author + '\'' +
        ", lastUpdated='" + lastUpdated + '\'' +
        ", description='" + description + '\'' +
        ", stars=" + stars +
        '}';
    }
}
