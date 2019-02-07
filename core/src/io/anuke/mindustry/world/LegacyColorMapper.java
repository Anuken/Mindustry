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

        map("ff0000", Blocks.stainedStone);
        map("00ff00", Blocks.stone);
        map("323232", Blocks.stone);
        map("646464", Blocks.stone, Blocks.rocks);
        map("50965a", Blocks.stainedStone);
        map("5ab464", Blocks.stainedStone, Blocks.stainedRocks);
        map("506eb4", Blocks.water);
        map("465a96", Blocks.deepwater);
        map("252525", Blocks.ignarock);
        map("575757", Blocks.ignarock, Blocks.duneRocks);
        map("988a67", Blocks.sand);
        map("e5d8bb", Blocks.sand, Blocks.duneRocks);
        map("c2d1d2", Blocks.snow);
        map("c4e3e7", Blocks.ice);
        map("f7feff", Blocks.snow, Blocks.snowrocks);
        map("6e501e", Blocks.stainedStone);
        map("ed5334", Blocks.ignarock);
        map("292929", Blocks.tar);
        map("c3a490", OreBlock.get(Blocks.stone, Items.copper));
        map("161616", OreBlock.get(Blocks.stone, Items.coal));
        map("6277bc", OreBlock.get(Blocks.stone, Items.titanium));
        map("83bc58", OreBlock.get(Blocks.stone, Items.thorium));
    }
    
    private void map(String color, Block block, Block wall){
        blockMap.put(Color.rgba8888(Color.valueOf(color)), new LegacyBlock(block, wall));
    }

    private void map(String color, Block block){
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
