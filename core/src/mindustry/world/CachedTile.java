package mindustry.world;

import arc.func.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.modules.*;

/**
 * A tile which does not trigger change events and whose entity types are cached.
 * Prevents garbage when loading previews.
 */
public class CachedTile extends Tile{

    public CachedTile(){
        super(0, 0);
    }

    @Override
    protected void preChanged(){
        //this basically overrides the old tile code and doesn't remove from proximity
    }

    @Override
    protected void changeEntity(Team team, Prov<Building> entityprov, int rotation){
        build = null;

        Block block = block();

        if(block.hasEntity()){
            Building n = entityprov.get();
            n.cons(new ConsumeModule(build));
            n.tile(this);
            n.block(block);
            if(block.hasItems) n.items = new ItemModule();
            if(block.hasLiquids) n.liquids(new LiquidModule());
            if(block.hasPower) n.power(new PowerModule());
            build = n;
        }
    }
}
