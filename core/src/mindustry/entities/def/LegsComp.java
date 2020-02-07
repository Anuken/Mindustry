package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;

@Component
abstract class LegsComp implements Posc, Flyingc, Hitboxc, DrawLayerGroundUnderc{
    transient float x, y;

    float baseRotation, walkTime;

    abstract TextureRegion legRegion();
    abstract TextureRegion baseRegion();

    @Override
    public void drawGroundUnder(){
        Draw.mixcol(Color.white, hitAlpha());

        float ft = Mathf.sin(walkTime * vel().len() * 5f, 6f, 2f + hitSize() / 15f);

        Floor floor = floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(legRegion(),
            x + Angles.trnsx(baseRotation, ft * i),
            y + Angles.trnsy(baseRotation, ft * i),
            legRegion().getWidth() * i * Draw.scl, legRegion().getHeight() * Draw.scl - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
        }

        if(floor.isLiquid){
            Draw.color(Color.white, floor.color, drownTime() * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion(), x, y, baseRotation - 90);

        Draw.mixcol();
    }
}
