package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.tilesize;

@Component
abstract class BlockUnitComp implements Unitc{
    @Import Team team;

    @ReadOnly transient Tilec tile;

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
        return tile == null || tile.dead();
    }

    @Replace
    public void team(Team team){
        if(tile != null && this.team != team){
            this.team = team;
            if(tile.team() != team){
                tile.team(team);
            }
        }
    }
}
