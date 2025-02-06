package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;

@Component
abstract class ChildComp implements Posc, Rotc{
    @Import float x, y, rotation;

    @Nullable Posc parent;
    boolean rotWithParent;
    float offsetX, offsetY, offsetPos, offsetRot;

    @Override
    public void add(){
        if(parent != null){
            offsetX = x - parent.getX();
            offsetY = y - parent.getY();
            if(rotWithParent){
                if(parent instanceof Rotc r){
                    offsetPos = -r.rotation();
                    offsetRot = rotation - r.rotation();
                }else if(parent instanceof RotBlock rot){
                    offsetPos = -rot.buildRotation();
                    offsetRot = rotation - rot.buildRotation();
                }
            }
        }
    }

    @Override
    public void update(){
        if(parent != null){
            if(rotWithParent){
                if(parent instanceof Rotc r){
                    x = parent.getX() + Angles.trnsx(r.rotation() + offsetPos, offsetX, offsetY);
                    y = parent.getY() + Angles.trnsy(r.rotation() + offsetPos, offsetX, offsetY);
                    rotation = r.rotation() + offsetRot;
                }else if(parent instanceof RotBlock rot){
                    x = parent.getX() + Angles.trnsx(rot.buildRotation() + offsetPos, offsetX, offsetY);
                    y = parent.getY() + Angles.trnsy(rot.buildRotation() + offsetPos, offsetX, offsetY);
                    rotation = rot.buildRotation() + offsetRot;
                }
            }else{
                x = parent.getX() + offsetX;
                y = parent.getY() + offsetY;
            }
        }
    }
}
