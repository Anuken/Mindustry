package mindustry.world.blocks.storage;

import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.world.*;

public class Vault extends StorageBlock{

    private Array<Tile> tmptiles = new Array<>();

    public Vault(String name){
        super(name);
        solid = true;
        update = true;
        destructible = true;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(tile.entity.proximity().count(t -> t.block() instanceof CoreBlock) == 0) return;
        Tile center = tile.getNearby(Mathf.random(0, 3));
        center.getLinkedTilesAs(tile.block(), tmptiles);

        for(Tile tmp : tmptiles){
            if(tmp == null) continue;
            if(tmp.link().block() != Blocks.air && tmp.link().block() != tile.block()) return;
        }

        Block block = tile.block();
        Team team = tile.getTeam();

        tile.removeNet();
        center.setNet(block, team, 0);
    }
}
