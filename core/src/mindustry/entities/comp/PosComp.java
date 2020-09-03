package mindustry.entities.comp;

import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.world;

@Component
abstract class PosComp implements Position{
    @SyncField(true) @SyncLocal float x, y;

    void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    void set(Position pos){
        set(pos.getX(), pos.getY());
    }

    void trns(float x, float y){
        set(this.x + x, this.y + y);
    }

    void trns(Position pos){
        trns(pos.getX(), pos.getY());
    }

    int tileX(){
        return Vars.world.toTile(x);
    }

    int tileY(){
        return Vars.world.toTile(y);
    }

    /** Returns air if this unit is on a non-air top block. */
    Floor floorOn(){
        Tile tile = tileOn();
        return tile == null || tile.block() != Blocks.air ? (Floor)Blocks.air : tile.floor();
    }

    Block blockOn(){
        Tile tile = tileOn();
        return tile == null ? Blocks.air : tile.block();
    }

    boolean onSolid(){
        Tile tile = tileOn();
        return tile != null && tile.solid();
    }

    @Nullable Tile tileOn(){
        return world.tileWorld(x, y);
    }

    @Override
    public float getX(){
        return x;
    }

    @Override
    public float getY(){
        return y;
    }
}
