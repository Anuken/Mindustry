package mindustry.mod;

import arc.struct.*;
import arc.util.*;
import mindustry.ui.dialogs.*;

/** Mod listing as a data class. */
public class ModListing{
    public String repo, name, internalName, author, lastUpdated, description, minGameVersion, version, iconHash = "";
    public boolean hasScripts, hasJava, iosCompatible, legacyCompatible, hasIcon;
    /** game build -> release ID + mod version */
    public @Nullable ArrayMap<String, ModRelease> releases;
    public String[] contentTypes = {};
    public int stars;

    @Override
    public String toString(){
        return "ModListing{" +
        "repo='" + repo + '\'' +
        ", name='" + name + '\'' +
        ", internalName='" + internalName + '\'' +
        ", author='" + author + '\'' +
        ", lastUpdated='" + lastUpdated + '\'' +
        ", description='" + description + '\'' +
        ", minGameVersion='" + minGameVersion + '\'' +
        ", hasScripts=" + hasScripts +
        ", hasJava=" + hasJava +
        ", stars=" + stars +
        '}';
    }

    public @Nullable ModRelease getMatchingRelease(){
        if(releases == null) return null;
        for(int i = 0; i < releases.size; i ++){
            String key = releases.keys[i];
            if(ModsDialog.matchesGameVersion(ModsDialog.parseVersion(key))){
                return releases.values[i];
            }
        }
        return null;
    }

    public static class ModRelease{
        /** Github release ID */
        public String id = "";
        /** Actual mod version string in release's mod.json */
        public String version = "";
    }
}
