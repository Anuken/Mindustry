package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class ElevationMoveComp implements Velc, Posc, Flyingc, Hitboxc{
    @Import float x, y;

    @Replace
    @Override
    public void move(float cx, float cy){
        if(isFlying()){
            x += cx;
            y += cy;
        }else{
            collisions.move(this, cx, cy);
        }
    }

    @Override
    public void update(){
        Tile tile = tileOn();

        if(!net.client() && tile != null && tile.solid() && !isFlying()){
            kill();
        }
    }

}
