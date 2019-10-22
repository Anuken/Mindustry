package io.anuke.mindustry.type;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ctype.Content;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.storage.*;

import static io.anuke.mindustry.Vars.*;

//TODO this class is a disappointment
public class Loadout extends Content{
    private final Array<Tile> outArray = new Array<>();
    private final IntMap<BlockEntry> entries = new IntMap<BlockEntry>(){{
        put('>', new BlockEntry(Blocks.conveyor, 0));
        put('^', new BlockEntry(Blocks.conveyor, 1));
        put('<', new BlockEntry(Blocks.conveyor, 2));
        put('v', new BlockEntry(Blocks.conveyor, 3));

        put('1', new BlockEntry(Blocks.coreShard));
        put('2', new BlockEntry(Blocks.coreFoundation));
        put('3', new BlockEntry(Blocks.coreNucleus));

        put('C', new BlockEntry(Blocks.mechanicalDrill, Blocks.oreCopper));
    }};

    private final IntMap<BlockEntry> blocks = new IntMap<>();
    private Block core;

    public Loadout(String... layout){
        int coreX = -1, coreY = -1;

        outer:
        for(int y = 0; y < layout.length; y++){
            for(int x = 0; x < layout[0].length(); x++){
                char c = layout[y].charAt(x);
                if(entries.get(c) != null && entries.get(c).block instanceof CoreBlock){
                    core = entries.get(c).block;
                    coreX = x;
                    coreY = y;
                    break outer;
                }
            }
        }

        if(coreX == -1) throw new IllegalArgumentException("Schematic does not have a core.");

        for(int y = 0; y < layout.length; y++){
            for(int x = 0; x < layout[0].length(); x++){
                char c = layout[y].charAt(x);
                if(entries.containsKey(c)){
                    BlockEntry entry = entries.get(c);
                    blocks.put(Pos.get(x - coreX, -(y - coreY)), entry);
                }
            }
        }
    }

    public Loadout(){

    }

    public Block core(){
        return core;
    }

    public void setup(int x, int y){
        for(IntMap.Entry<BlockEntry> entry : blocks.entries()){
            int rx = Pos.x(entry.key);
            int ry = Pos.y(entry.key);
            Tile tile = world.tile(x + rx, y + ry);
            if(tile == null) continue;

            world.setBlock(tile, entry.value.block, defaultTeam);
            tile.rotation((byte)entry.value.rotation);
            if(entry.value.ore != null){
                for(Tile t : tile.getLinkedTiles(outArray)){
                    t.setOverlay(entry.value.ore);
                }
            }
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.loadout;
    }

    static class BlockEntry{
        final Block block;
        final Block ore;
        final int rotation;

        BlockEntry(Block block, Block ore){
            this.block = block;
            this.ore = ore;
            this.rotation = 0;
        }

        BlockEntry(Block block, int rotation){
            this.block = block;
            this.ore = null;
            this.rotation = rotation;
        }

        BlockEntry(Block block){
            this.block = block;
            this.ore = null;
            this.rotation = 0;
        }
    }

}
