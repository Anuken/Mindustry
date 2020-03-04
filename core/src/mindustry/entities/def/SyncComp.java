package mindustry.entities.def;

import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.net.*;

@Component
abstract class SyncComp implements Posc{
    @Import float x, y;

    Interpolator interpolator = new Interpolator();

    void setNet(float x, float y){
        set(x, y);

        //TODO change interpolator API
        interpolator.target.set(x, y);
        interpolator.last.set(x, y);
        interpolator.pos.set(0, 0);
        interpolator.updateSpacing = 16;
        interpolator.lastUpdated = 0;
    }

    @Override
    public void update(){
        if(Vars.net.client() && !isLocal()){
            interpolate();
        }
    }

    void interpolate(){
        interpolator.update();
        x = interpolator.pos.x;
        y = interpolator.pos.y;
    }
}
