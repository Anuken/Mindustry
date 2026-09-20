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
    public String[] tags = {};
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

    /** @return the specific release that matches the current game version, or null if no specific match is found (i.e. /latest should be used) */
    public @Nullable ModRelease getMatchingRelease(){
        if(releases == null) return null;
        for(var entry : releases){
            if(ModsDialog.matchesGameVersion(ModsDialog.parseVersion(entry.key))){
                return entry.value;
            }
        }
        return null;
    }

    public static class ModRelease{
        /** Github release ID ($API/releases/$ID) */
        public String id = "";
        /** Actual mod version string in release's mod.json */
        public String version = "";

        @Override
        public String toString(){
            return "ModRelease{" +
            "id='" + id + '\'' +
            ", version='" + version + '\'' +
            '}';
        }
    }
}
