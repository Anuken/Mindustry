package io.anuke.mindustry.world;

import io.anuke.arc.collection.IntMap;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.modules.*;

/**
 * A tile which does not trigger change events and whose entity types are cached.
 * Prevents garbage when loading previews.
 */
public class CachedTile extends Tile{
    private static IntMap<TileEntity> entities = new IntMap<>();

    public CachedTile(){
        super(0, 0);
    }

    @Override
    public Team getTeam(){
        return Team.all[getTeamID()];
    }

    @Override
    protected void preChanged(){
        super.setTeam(Team.none);
    }

    @Override
    protected void changed(){
        entity = null;

        Block block = block();

        if(block.hasEntity()){
            //cache all entity types so only one is ever created per block type. do not add it.
            if(!entities.containsKey(block.id)){
                TileEntity n = block.newEntity();
                n.cons = new ConsumeModule(entity);
                n.tile = this;
                if(block.hasItems) n.items = new ItemModule();
                if(block.hasLiquids) n.liquids = new LiquidModule();
                if(block.hasPower) n.power = new PowerModule();
                entities.put(block.id, n);
            }

            entity = entities.get(block.id);

        }
    }
}
