package mindustry.world.blocks.logic;

import arc.graphics.g2d.*;
import arc.util.io.*;
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
        drawDisabled = false;
        autoResetEnabled = false;

        config(Boolean.class, (SwitchBuild entity, Boolean b) -> entity.enabled = b);
    }

    public class SwitchBuild extends Building{

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.enabled) return enabled ? 1 : 0;
            return super.sense(sensor);
        }

        @Override
        public boolean configTapped(){
            configure(!enabled);
            Sounds.click.at(this);
            return false;
        }

        @Override
        public void draw(){
            super.draw();

            if(enabled){
                Draw.rect(onRegion, x, y);
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void readAll(Reads read, byte revision){
            super.readAll(read, revision);

            if(revision == 1){
                enabled = read.bool();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.bool(enabled);
        }
    }
}
