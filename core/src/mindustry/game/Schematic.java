package mindustry.game;

import arc.struct.*;
import arc.struct.IntIntMap.*;
import arc.files.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class Schematic implements Publishable, Comparable<Schematic>{
    public final Array<Stile> tiles;
    public StringMap tags;
    public int width, height;
    public @Nullable
    Fi file;
    /** Associated mod. If null, no mod is associated with this schematic. */
    public @Nullable LoadedMod mod;

    public Schematic(Array<Stile> tiles, @NonNull StringMap tags, int width, int height){
        this.tiles = tiles;
        this.tags = tags;
        this.width = width;
        this.height = height;
    }

    public Array<ItemStack> requirements(){
        IntIntMap amounts = new IntIntMap();

        tiles.each(t -> {
            for(ItemStack stack : t.block.requirements){
                amounts.getAndIncrement(stack.item.id, 0, stack.amount);
            }
        });
        Array<ItemStack> stacks = new Array<>();
        for(Entry ent : amounts.entries()){
            stacks.add(new ItemStack(Vars.content.item(ent.key), ent.value));
        }
        stacks.sort();
        return stacks;
    }

    public boolean hasCore(){
        return tiles.contains(s -> s.block instanceof CoreBlock);
    }

    public @NonNull CoreBlock findCore(){
        CoreBlock block = (CoreBlock)tiles.find(s -> s.block instanceof CoreBlock).block;
        if(block == null) throw new IllegalArgumentException("Schematic is missing a core!");
        return block;
    }

    public String name(){
        return tags.get("name", "unknown");
    }

    public void save(){
        schematics.saveChanges(this);
    }

    @Override
    public String getSteamID(){
        return tags.get("steamid");
    }

    @Override
    public void addSteamID(String id){
        tags.put("steamid", id);
        save();
    }

    @Override
    public void removeSteamID(){
        tags.remove("steamid");
        save();
    }

    @Override
    public String steamTitle(){
        return name();
    }

    @Override
    public String steamDescription(){
        return null;
    }

    @Override
    public String steamTag(){
        return "schematic";
    }

    @Override
    public Fi createSteamFolder(String id){
        Fi directory = tmpDirectory.child("schematic_" + id).child("schematic." + schematicExtension);
        file.copyTo(directory);
        return directory;
    }

    @Override
    public Fi createSteamPreview(String id){
        Fi preview = tmpDirectory.child("schematic_preview_" + id + ".png");
        schematics.savePreview(this, preview);
        return preview;
    }

    @Override
    public int compareTo(Schematic schematic){
        return name().compareTo(schematic.name());
    }

    public static class Stile{
        public @NonNull Block block;
        public short x, y;
        public int config;
        public byte rotation;

        public Stile(Block block, int x, int y, int config, byte rotation){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
            this.config = config;
            this.rotation = rotation;
        }
    }
}
