package io.anuke.mindustry.game;

import io.anuke.arc.collection.*;
import io.anuke.arc.collection.IntIntMap.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

public class Schematic{
    public final Array<Stile> tiles;
    public StringMap tags;
    public int width, height;
    public @Nullable FileHandle file;

    public Schematic(Array<Stile> tiles, StringMap tags, int width, int height){
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

    public String name(){
        return tags.get("name", "unknown");
    }

    public boolean isWorkshop(){
        return tags.containsKey("workshop");
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
