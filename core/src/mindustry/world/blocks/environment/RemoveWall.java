package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

public class RemoveWall extends Block{

    public RemoveWall(String name){
        super(name);

        allowRectanglePlacement = true;
        placeEffect = Fx.rotateBlock;
        instantBuild = true;
        ignoreBuildDarkness = true;
        placeableLiquid = true;
        inEditor = false;
    }

    @Override
    public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid, float alpha){
        Draw.reset();
        Draw.alpha(alpha * (valid ? 1f : 0.2f));
        float prevScale = Draw.scl;
        Draw.scl *= plan.animScale;
        drawPlanRegion(plan, list);
        Draw.scl = prevScale;
        Draw.reset();
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return tile.block() != Blocks.air;
    }

    @Override
    public boolean canReplace(Block other){
        return other != Blocks.air && !other.synthetic();
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, Object config){
        tile.setBlock(Blocks.air);
        if(tile.overlay().wallOre){
            tile.setOverlay(Blocks.air);
        }
    }

}
