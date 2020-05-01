package mindustry.entities.def;

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
    float x, y;

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

    int tileX(){
        return Vars.world.toTile(x);
    }

    int tileY(){
        return Vars.world.toTile(y);
    }

    /** Returns air if this unit is on a non-air top block. */
    public Floor floorOn(){
        Tile tile = tileOn();
        return tile == null || tile.block() != Blocks.air ? (Floor)Blocks.air : tile.floor();
    }

    public @Nullable
    Tile tileOn(){
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
