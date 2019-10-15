package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.Units;

public class Revenant extends FlyingUnit{

    @Override
    public void drawWeapons(){
        for(int i : Mathf.signs){
            float tra = rotation - 90, trY = -getWeapon().getRecoil(this, i > 0) + type.weaponOffsetY;
            float w = i > 0 ? -12 : 12;
            float wx = x + Angles.trnsx(tra, getWeapon().width * i, trY), wy = y + Angles.trnsy(tra, getWeapon().width * i, trY);
            int wi = (i + 1) / 2;
            Draw.rect(getWeapon().region, wx, wy, w, 12, weaponAngles[wi] - 90);
        }
    }

    @Override
    protected void attack(float circleLength){
        moveTo(circleLength);
    }

    @Override
    protected void updateRotation(){
        if(!Units.invalidateTarget(target, this)){
            rotation = Mathf.slerpDelta(rotation, angleTo(target), type.rotatespeed);
        }else{
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), type.baseRotateSpeed);
        }
    }
}
