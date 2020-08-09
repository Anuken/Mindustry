package mindustry.game;

import arc.files.*;
import arc.struct.*;
import arc.struct.IntIntMap.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Schematic implements Publishable, Comparable<Schematic>{
    public final Seq<Stile> tiles;
    public StringMap tags;
    public int width, height;
    public @Nullable Fi file;
    /** Associated mod. If null, no mod is associated with this schematic. */
    public @Nullable LoadedMod mod;

    public Schematic(Seq<Stile> tiles, @NonNull StringMap tags, int width, int height){
        this.tiles = tiles;
        this.tags = tags;
        this.width = width;
        this.height = height;
    }

    public float powerProduction(){
        return tiles.sumf(s -> s.block instanceof PowerGenerator ? ((PowerGenerator)s.block).powerProduction : 0f);
    }

    public float powerConsumption(){
        return tiles.sumf(s -> s.block.consumes.has(ConsumeType.power) ? s.block.consumes.getPower().usage : 0f);
    }

    public Seq<ItemStack> requirements(){
        IntIntMap amounts = new IntIntMap();

        tiles.each(t -> {
            if(t.block.buildVisibility == BuildVisibility.hidden) return;

            for(ItemStack stack : t.block.requirements){
                amounts.increment(stack.item.id, stack.amount);
            }
        });
        Seq<ItemStack> stacks = new Seq<>();
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
        Stile tile = tiles.find(s -> s.block instanceof CoreBlock);
        if(tile == null) throw new IllegalArgumentException("Schematic is missing a core!");
        return (CoreBlock)tile.block;
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
        public Object config;
        public byte rotation;

        public Stile(Block block, int x, int y, Object config, byte rotation){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
            this.config = config;
            this.rotation = rotation;
        }

        //pooling only
        public Stile(){
            block = Blocks.air;
        }

        public Stile set(Stile other){
            block = other.block;
            x = other.x;
            y = other.y;
            config = other.config;
            rotation = other.rotation;
            return this;
        }

        public Stile copy(){
            return new Stile(block, x, y, config, rotation);
        }
    }
}
