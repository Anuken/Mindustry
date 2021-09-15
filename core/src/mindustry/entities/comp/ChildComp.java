package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class ChildComp implements Posc{
    @Import float x, y;

    @Nullable Posc parent;
    boolean rotateWithParent;
    float offsetX, offsetY, offsetRot;

    @Override
    public void add(){
        if(parent != null){
            offsetX = x - parent.getX();
            offsetY = y - parent.getY();
            if(rotateWithParent && parent instanceof Rotc r){
                offsetRot = -r.rotation();
            }
        }
    }

    @Override
    public void update(){
        if(parent != null){
            if(rotateWithParent && parent instanceof Rotc r){
                x = parent.getX() + Angles.trnsx(r.rotation() + offsetRot, offsetX, offsetY);
                y = parent.getY() + Angles.trnsy(r.rotation() + offsetRot, offsetX, offsetY);
            }else{
                x = parent.getX() + offsetX;
                y = parent.getY() + offsetY;
            }
        }
    }
}
