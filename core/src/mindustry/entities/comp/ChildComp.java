package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.BaseTurret.*;

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
                }else if(parent instanceof BaseTurretBuild build){
                    offsetPos = -build.rotation;
                    offsetRot = rotation - build.rotation;
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
                }else if(parent instanceof BaseTurretBuild build){
                    x = parent.getX() + Angles.trnsx(build.rotation + offsetPos, offsetX, offsetY);
                    y = parent.getY() + Angles.trnsy(build.rotation + offsetPos, offsetX, offsetY);
                    rotation = build.rotation + offsetRot;
                }
            }else{
                x = parent.getX() + offsetX;
                y = parent.getY() + offsetY;
            }
        }
    }
}
