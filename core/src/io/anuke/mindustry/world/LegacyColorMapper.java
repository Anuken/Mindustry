package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.OreBlocks;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.blocks.Floor;

public class LegacyColorMapper implements ContentList{
    private static IntMap<LegacyBlock> blockMap = new IntMap<>();
    private static LegacyBlock defaultValue;

    public static LegacyBlock get(int color){
        return blockMap.get(color, defaultValue);
    }

    @Override
    public void load(){
        defaultValue = new LegacyBlock(Blocks.stone, 0);

        insert("ff0000", Blocks.dirt, 0);
        insert("00ff00", Blocks.stone, 0);
        insert("323232", Blocks.stone, 0);
        insert("646464", Blocks.stone, 1);
        insert("50965a", Blocks.grass, 0);
        insert("5ab464", Blocks.grass, 1);
        insert("506eb4", Blocks.water, 0);
        insert("465a96", Blocks.deepwater, 0);
        insert("252525", Blocks.blackstone, 0);
        insert("575757", Blocks.blackstone, 1);
        insert("988a67", Blocks.sand, 0);
        insert("e5d8bb", Blocks.sand, 1);
        insert("c2d1d2", Blocks.snow, 0);
        insert("c4e3e7", Blocks.ice, 0);
        insert("f7feff", Blocks.snow, 1);
        insert("6e501e", Blocks.dirt, 0);
        insert("ed5334", Blocks.lava, 0);
        insert("292929", Blocks.oil, 0);
        insert("c3a490", OreBlocks.get(Blocks.stone, Items.copper), 0);
        insert("161616", OreBlocks.get(Blocks.stone, Items.coal), 0);
        insert("6277bc", OreBlocks.get(Blocks.stone, Items.titanium), 0);
        insert("83bc58", OreBlocks.get(Blocks.stone, Items.thorium), 0);
    }

    @Override
    public ContentType type(){
        return ContentType.block;
    }
    
    private void insert(String color, Block block, int elevation){
        blockMap.put(Color.rgba8888(Color.valueOf(color)), new LegacyBlock(block, elevation));
    }

    public static class LegacyBlock{
        public final int elevation;
        public final Floor floor;

        public LegacyBlock(Block floor, int elevation){
            this.elevation = elevation;
            this.floor = (Floor) floor;
        }
    }

}
