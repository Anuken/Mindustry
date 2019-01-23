package io.anuke.mindustry.world;

import io.anuke.arc.collection.IntMap;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

public class LegacyColorMapper implements ContentList{
    private static IntMap<LegacyBlock> blockMap = new IntMap<>();
    private static LegacyBlock defaultValue;

    public static LegacyBlock get(int color){
        return blockMap.get(color, defaultValue);
    }

    @Override
    public void load(){
        defaultValue = new LegacyBlock(Blocks.stone, Blocks.air);

        map("ff0000", Blocks.dirt, 0);
        map("00ff00", Blocks.stone, 0);
        map("323232", Blocks.stone, 0);
        map("646464", Blocks.stone, 1);
        map("50965a", Blocks.grass, 0);
        map("5ab464", Blocks.grass, 1);
        map("506eb4", Blocks.water, 0);
        map("465a96", Blocks.deepwater, 0);
        map("252525", Blocks.blackstone, 0);
        map("575757", Blocks.blackstone, 1);
        map("988a67", Blocks.sand, 0);
        map("e5d8bb", Blocks.sand, 1);
        map("c2d1d2", Blocks.snow, 0);
        map("c4e3e7", Blocks.ice, 0);
        map("f7feff", Blocks.snow, 1);
        map("6e501e", Blocks.dirt, 0);
        map("ed5334", Blocks.blackstone, 0);
        map("292929", Blocks.tar, 0);
        map("c3a490", OreBlock.get(Blocks.stone, Items.copper), 0);
        map("161616", OreBlock.get(Blocks.stone, Items.coal), 0);
        map("6277bc", OreBlock.get(Blocks.stone, Items.titanium), 0);
        map("83bc58", OreBlock.get(Blocks.stone, Items.thorium), 0);
    }
    
    private void map(String color, Block block, Block wall){
        blockMap.put(Color.rgba8888(Color.valueOf(color)), new LegacyBlock(block, wall));
    }

    //todo fix this, implement proper mapping w/ walls
    private void map(String color, Block block, int __TODO_fix_this){
        blockMap.put(Color.rgba8888(Color.valueOf(color)), new LegacyBlock(block, Blocks.air));
    }

    public static class LegacyBlock{
        public final Floor floor;
        public final Block wall;

        public LegacyBlock(Block floor, Block wall){
            this.floor = (Floor) floor;
            this.wall = wall;
        }
    }

}
