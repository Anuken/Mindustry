package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.StringMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Texture;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.DefaultWaves;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.io.JsonIO;

public class Map implements Comparable<Map>{
    /** Whether this is a custom map. */
    public final boolean custom;
    /** Metadata. Author description, display name, etc. */
    public final StringMap tags;
    /** Base file of this map. File can be named anything at all. */
    public final FileHandle file;
    /** Format version. */
    public final int version;
    /** Map width/height, shorts. */
    public int width, height;
    /** Preview texture. */
    public Texture texture;
    /** Build that this map was created in. -1 = unknown or custom build. */
    public int build;

    public Map(FileHandle file, int width, int height, StringMap tags, boolean custom, int version, int build){
        this.custom = custom;
        this.tags = tags;
        this.file = file;
        this.width = width;
        this.height = height;
        this.version = version;
        this.build = build;
    }

    public Map(FileHandle file, int width, int height, StringMap tags, boolean custom, int version){
        this(file, width, height, tags, custom, version, -1);
    }

    public Map(FileHandle file, int width, int height, StringMap tags, boolean custom){
        this(file, width, height, tags, custom, -1);
    }

    public Map(StringMap tags){
        this(Vars.customMapDirectory.child(tags.get("name", "unknown")), 0, 0, tags, true);
    }

    public int getHightScore(){
        return Core.settings.getInt("hiscore" + file.nameWithoutExtension(), 0);
    }

    public void setHighScore(int score){
        Core.settings.put("hiscore" + file.nameWithoutExtension(), score);
        Vars.data.modified();
    }

    /** This creates a new instance.*/
    public Rules rules(){
        Rules result = JsonIO.read(Rules.class, tags.get("rules", "{}"));
        if(result.spawns.isEmpty()) result.spawns = DefaultWaves.get();
        return result;
    }

    /** Whether this map has a core of the enemy 'wave' team. Default: true.
     * Used for checking Attack mode validity.*/
    public boolean hasEnemyCore(){
        return tags.get("enemycore", "true").equals("true");
    }

    /** Whether this map has a core of any team except the default player team. Default: true.
     * Used for checking PvP mode validity.*/
    public boolean hasOtherCores(){
        return tags.get("othercore", "true").equals("true");
    }

    public String author(){
        return tag("author");
    }

    public String description(){
        return tag("description");
    }

    public String name(){
        return tag("name");
    }

    public String tag(String name){
        return tags.containsKey(name) && !tags.get(name).trim().isEmpty() ? tags.get(name) : Core.bundle.get("unknown");
    }

    public boolean hasTag(String name){
        return tags.containsKey(name);
    }

    @Override
    public int compareTo(Map map){
        int type = -Boolean.compare(custom, map.custom);
        if(type != 0){
            return type;
        }else{
            return name().compareTo(map.name());
        }
    }

    @Override
    public String toString(){
        return "Map{" +
        "file='" + file + '\'' +
        ", custom=" + custom +
        ", tags=" + tags +
        '}';
    }
}
