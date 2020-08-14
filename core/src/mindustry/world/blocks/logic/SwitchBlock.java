package mindustry.world.blocks.logic;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.*;

public class SwitchBlock extends Block{
    public @Load("@-on") TextureRegion onRegion;

    public SwitchBlock(String name){
        super(name);
        configurable = true;
        update = true;

        config(Boolean.class, (SwitchBuild entity, Boolean b) -> entity.on = b);
    }

    public class SwitchBuild extends Building{
        public boolean on;

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.enabled) return on ? 1 : 0;
            return super.sense(sensor);
        }

        @Override
        public boolean configTapped(){
            configure(!on);
            Sounds.click.at(this);
            return false;
        }

        @Override
        public void draw(){
            super.draw();

            if(on){
                Draw.rect(onRegion, x, y);
            }
        }
    }
}
