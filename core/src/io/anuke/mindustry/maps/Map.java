package io.anuke.mindustry.maps;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.filters.*;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.*;

public class Map implements Comparable<Map>, Publishable{
    /** Whether this is a custom map. */
    public final boolean custom;
    /** Metadata. Author description, display name, etc. */
    public final StringMap tags;
    /** Base file of this map. File can be named anything at all. */
    public final FileHandle file;
    /** Format version. */
    public final int version;
    /** Whether this map is managed, e.g. downloaded from the Steam workshop.*/
    public boolean workshop;
    /** Map width/height, shorts. */
    public int width, height;
    /** Preview texture. */
    public Texture texture;
    /** Build that this map was created in. -1 = unknown or custom build. */
    public int build;
    /** All teams present on this map.*/
    public IntSet teams = new IntSet();
    /** Number of enemy spawns on this map.*/
    public int spawns = 0;

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

    public Texture safeTexture(){
        return texture == null ? Core.assets.get("sprites/error.png") : texture;
    }

    public FileHandle previewFile(){
        return Vars.mapPreviewDirectory.child((workshop ? file.parent().name() : file.nameWithoutExtension()) + ".png");
    }

    public FileHandle cacheFile(){
        return Vars.mapPreviewDirectory.child(file.nameWithoutExtension() + "-cache.dat");
    }

    public void setHighScore(int score){
        Core.settings.put("hiscore" + file.nameWithoutExtension(), score);
        Vars.data.modified();
    }

    /** Returns the result of applying this map's rules to the specified gamemode.*/
    public Rules applyRules(Gamemode mode){
        //mode specific defaults have been applied
        Rules out = new Rules();
        mode.apply(out);

        //now apply map-specific overrides
        return rules(out);
    }

    /** This creates a new instance of Rules.*/
    public Rules rules(){
        return rules(new Rules());
    }

    public Rules rules(Rules base){
        try{
            Rules result = JsonIO.read(Rules.class, base, tags.get("rules", "{}"));
            if(result.spawns.isEmpty()) result.spawns = Vars.defaultWaves.get();
            return result;
        }catch(Exception e){
            //error reading rules. ignore?
            e.printStackTrace();
            return new Rules();
        }
    }

    /** Returns the generation filters that this map uses on load.*/
    public Array<GenerateFilter> filters(){
        if(tags.getInt("build", -1) < 83 && tags.getInt("build", -1) != -1 && tags.get("genfilters", "").isEmpty()){
            return Array.with();
        }
        return maps.readFilters(tags.get("genfilters", ""));
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
    public String getSteamID(){
        return tags.get("steamid");
    }

    @Override
    public void addSteamID(String id){
        tags.put("steamid", id);

        ui.editor.editor.getTags().put("steamid", id);
        try{
            ui.editor.save();
        }catch(Exception e){
            Log.err(e);
        }
        Events.fire(new MapPublishEvent());
    }

    @Override
    public void removeSteamID(){
        tags.remove("steamid");

        ui.editor.editor.getTags().remove("steamid");
        try{
            ui.editor.save();
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public String steamTitle(){
        return name();
    }

    @Override
    public String steamDescription(){
        return description();
    }

    @Override
    public String steamTag(){
        return "map";
    }

    @Override
    public FileHandle createSteamFolder(String id){
        return null;
    }

    @Override
    public FileHandle createSteamPreview(String id){
        return null;
    }

    @Override
    public Array<String> extraTags(){
        Gamemode mode = Gamemode.attack.valid(this) ? Gamemode.attack : Gamemode.survival;
        return Array.with(mode.name());
    }

    @Override
    public boolean prePublish(){
        tags.put("author", player.name);
        ui.editor.editor.getTags().put("author", tags.get("author"));
        ui.editor.save();

        return true;
    }

    @Override
    public int compareTo(Map map){
        int work = -Boolean.compare(workshop, map.workshop);
        if(work != 0) return work;
        int type = -Boolean.compare(custom, map.custom);
        if(type != 0) return type;
        int modes = Boolean.compare(Gamemode.pvp.valid(this), Gamemode.pvp.valid(map));
        if(modes != 0) return modes;

        return name().compareTo(map.name());
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
