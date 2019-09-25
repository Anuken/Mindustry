package io.anuke.mindustry.world;

import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.modules.*;

/**
 * A tile which does not trigger change events and whose entity types are cached.
 * Prevents garbage when loading previews.
 */
public class CachedTile extends Tile{

    public CachedTile(){
        super(0, 0);
    }

    @Override
    public Team getTeam(){
        return Team.all[getTeamID()];
    }

    @Override
    protected void preChanged(){
        //this basically overrides the old tile code and doesn't remove from proximity
        team = 0;
    }

    @Override
    protected void changed(){
        entity = null;

        Block block = block();

        if(block.hasEntity()){
            TileEntity n = block.newEntity();
            n.cons = new ConsumeModule(entity);
            n.tile = this;
            n.block = block;
            if(block.hasItems) n.items = new ItemModule();
            if(block.hasLiquids) n.liquids = new LiquidModule();
            if(block.hasPower) n.power = new PowerModule();
            entity = n;
        }
    }
}
