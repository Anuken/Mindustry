package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.world.Block;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.world;

public class BlockLocMission extends Mission{
    private final Block block;
    private final int x, y, rotation;

    public BlockLocMission(Block block, int x, int y, int rotation){
        this.block = block;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public BlockLocMission(Block block, int x, int y){
        this.block = block;
        this.x = x;
        this.y = y;
        this.rotation = 0;
    }

    @Override
    public boolean isComplete(){
        return world.tile(x, y).block() == block && (!block.rotate || world.tile(x,y).getRotation() == rotation);
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.block", block.formalName);
    }
}
