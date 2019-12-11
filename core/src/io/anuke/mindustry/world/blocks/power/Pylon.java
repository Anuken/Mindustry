package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;

import static io.anuke.mindustry.Vars.*;

public class Pylon extends PowerNode{
    public Pylon(String name){
        super(name);
        entityType = PylonEntity::new;
    }

    class PylonEntity extends TileEntity implements ProjectorTrait{

        float realRadius(){
            return laserRange * tilesize;
        }

        @Override
        public void drawOver(){
            Draw.color(Color.white);
            Draw.alpha(1f - power.status);
            Fill.circle(x, y, realRadius());
            Draw.color();
        }

        @Override
        public void drawSimple(){
            float rad = realRadius();
            Draw.color(color);
            Lines.stroke(1.5f);
            Draw.alpha(0.17f);
            Fill.circle(x, y, rad);
            Draw.alpha(1f);
            Lines.circle(x, y, rad);
            Draw.reset();
        }

        @Override
        public Color accent(){
            return Color.valueOf("add8e6");
        }

        @Override
        public String projectorSet(){
            return "pylonSet";
        }

        @Override
        public void draw(){
            Draw.color(color);
            Fill.circle(x, y, realRadius());
            Draw.color();
        }

        @Override
        public EntityGroup targetGroup(){
            return projectorGroup;
        }
    }
}
