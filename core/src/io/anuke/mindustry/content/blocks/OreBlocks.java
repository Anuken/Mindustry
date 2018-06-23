package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

public class OreBlocks extends BlockList {
    private static final ObjectMap<Item, ObjectMap<Block, Block>> oreBlockMap = new ObjectMap<>();

    @Override
    public void load() {
        Item[] ores = {Items.tungsten, Items.lead, Items.coal, Items.titanium, Items.thorium};

        for(Item item : ores){
            ObjectMap<Block, Block> map = new ObjectMap<>();
            oreBlockMap.put(item, map);

            for(Block block : Block.all()){
                if(block instanceof Floor && ((Floor) block).hasOres){
                    map.put(block, new OreBlock(item, (Floor) block));
                }
            }
        }
    }

    public static Block get(Block floor, Item item){
        if(!oreBlockMap.containsKey(item)) throw new IllegalArgumentException("Item '" + item + "' is not an ore!");
        if(!oreBlockMap.get(item).containsKey(floor)) throw new IllegalArgumentException("Block '" + floor.name + "' does not support ores!");
        return oreBlockMap.get(item).get(floor);
    }
}
