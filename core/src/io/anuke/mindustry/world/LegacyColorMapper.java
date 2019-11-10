package io.anuke.mindustry.world;

import io.anuke.arc.collection.IntMap;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.ctype.ContentList;
import io.anuke.mindustry.world.blocks.Floor;

public class LegacyColorMapper implements ContentList{
    private static IntMap<LegacyBlock> blockMap = new IntMap<>();
    private static LegacyBlock defaultValue;

    public static LegacyBlock get(int color){
        return blockMap.get(color, defaultValue);
    }

    @Override
    public void load(){
        defaultValue = new LegacyBlock(Blocks.stone, Blocks.air);

        map("ff0000", Blocks.stone, Blocks.air, Blocks.spawn);
        map("00ff00", Blocks.stone);
        map("323232", Blocks.stone);
        map("646464", Blocks.stone, Blocks.rocks);
        map("50965a", Blocks.grass);
        map("5ab464", Blocks.grass, Blocks.pine);
        map("506eb4", Blocks.water);
        map("465a96", Blocks.deepwater);
        map("252525", Blocks.ignarock);
        map("575757", Blocks.ignarock, Blocks.duneRocks);
        map("988a67", Blocks.sand);
        map("e5d8bb", Blocks.sand, Blocks.duneRocks);
        map("c2d1d2", Blocks.snow);
        map("c4e3e7", Blocks.ice);
        map("f7feff", Blocks.snow, Blocks.snowrocks);
        map("6e501e", Blocks.holostone);
        map("ed5334", Blocks.magmarock);
        map("292929", Blocks.tar);
        map("c3a490", Blocks.stone, Blocks.air, Blocks.oreCopper);
        map("161616", Blocks.stone, Blocks.air, Blocks.oreCoal);
        map("6277bc", Blocks.stone, Blocks.air, Blocks.oreTitanium);
        map("83bc58", Blocks.stone, Blocks.air, Blocks.oreThorium);
    }

    private void map(String color, Block block, Block wall, Block ore){
        blockMap.put(Color.rgba8888(Color.valueOf(color)), new LegacyBlock(block, wall, ore));
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
        public final Block ore;

        public LegacyBlock(Block floor, Block wall){
            this(floor, wall, Blocks.air);
        }

        public LegacyBlock(Block floor, Block wall, Block ore){
            this.floor = (Floor)floor;
            this.wall = wall;
            this.ore = ore;
        }
    }

}
