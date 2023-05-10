package mindustry.entities.comp;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@Component
abstract class BlockUnitComp implements Unitc{
    @Import Team team;

    @ReadOnly transient Building tile;

    public void tile(Building tile){
        this.tile = tile;

        //sets up block stats
        maxHealth(tile.block.health);
        health(tile.health);
        hitSize(tile.block.size * tilesize * 0.7f);
        set(tile);
    }

    @Override
    public void add(){
        if(tile == null){
            throw new RuntimeException("Do not add BlockUnit entities to the game, they will simply crash. Internal use only.");
        }
    }

    @Override
    public void update(){
        if(tile != null){
            team = tile.team;
        }
    }

    @Replace
    @Override
    public TextureRegion icon(){
        return tile.block.fullIcon;
    }

    @Override
    public void killed(){
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
    public boolean isValid(){
        return tile != null && tile.isValid();
    }

    @Replace
    public void team(Team team){
        if(tile != null && this.team != team){
            this.team = team;
            if(tile.team != team){
                tile.changeTeam(team);
            }
        }
    }
}
