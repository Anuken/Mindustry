package mindustry.mod;

/** Mod listing as a data class. */
public class ModListing{
    public String repo, name, modName, subtitle, author, lastUpdated, description,  minGameVersion;
    public boolean hasScripts, hasJava;
    public String[] contentTypes = {};
    public int stars;

    @Override
    public String toString(){
        return "ModListing{" +
        "repo='" + repo + '\'' +
        ", name='" + name + '\'' +
        ", modName='" + modName + '\'' +
        ", author='" + author + '\'' +
        ", lastUpdated='" + lastUpdated + '\'' +
        ", description='" + description + '\'' +
        ", minGameVersion='" + minGameVersion + '\'' +
        ", hasScripts=" + hasScripts +
        ", hasJava=" + hasJava +
        ", stars=" + stars +
        '}';
    }
}
