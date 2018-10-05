package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.game.EventType.BlockBuildEvent;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.defaultTeam;
import static io.anuke.mindustry.Vars.world;

/**A mission in which the player must place a block somewhere.*/
public class BlockMission extends Mission{
    private final Block block;
    private boolean complete;

    static{
        Events.on(BlockBuildEvent.class, event -> {
            if(world.getSector() != null && event.team == defaultTeam){
                Mission mission = world.getSector().currentMission();
                if(mission instanceof BlockMission){
                    BlockMission block = (BlockMission)world.getSector().currentMission();
                    if(block.block == event.tile.block()){
                        block.complete = true;
                    }
                }
            }
        });
    }

    public BlockMission(Block block){
        this.block = block;
    }

    @Override
    public void reset(){
        complete = false;
    }

    @Override
    public boolean isComplete(){
        return complete;
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.block", block.formalName);
    }

    @Override
    public GameMode getMode(){
        return GameMode.noWaves;
    }
}
