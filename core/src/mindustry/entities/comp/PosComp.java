package mindustry.entities.comp;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

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
        return World.toTile(x);
    }

    int tileY(){
        return World.toTile(y);
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

    @Nullable
    Building buildOn(){
        return world.buildWorld(x, y);
    }

    @Nullable
    Tile tileOn(){
        return world.tileWorld(x, y);
    }

    boolean onSolid(){
        Tile tile = tileOn();
        return tile == null || tile.solid();
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
