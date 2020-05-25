package mindustry.entities.comp;

import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.tilesize;

@Component
abstract class BlockUnitComp implements Unitc{
    @ReadOnly @NonNull Tilec tile;

    public void tile(Tilec tile){
        this.tile = tile;

        //sets up block stats
        maxHealth(tile.block().health);
        health(tile.health());
        hitSize(tile.block().size * tilesize);
        set(tile);
    }

    @Replace
    public void kill(){
        tile.kill();
    }

    @Replace
    public void damage(float v, boolean b){
        tile.damage(v, b);
    }

    @Replace
    public boolean dead(){
        return tile.dead();
    }

    @Replace
    public void team(Team team){
        tile.team(team);
    }
}
