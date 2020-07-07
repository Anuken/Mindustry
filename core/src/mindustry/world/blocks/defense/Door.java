package mindustry.world.blocks.defense;

import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

import static mindustry.Vars.pathfinder;

public class Door extends Wall{
    protected final static Rect rect = new Rect();

    public final int timerToggle = timers++;
    public Effect openfx = Fx.dooropen;
    public Effect closefx = Fx.doorclose;
    public @Load("@-open") TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;

        config(Boolean.class, (DoorEntity entity, Boolean open) -> {
            entity.open = open;
            pathfinder.updateTile(entity.tile());
            (open ? closefx : openfx).at(entity);
            Sounds.door.at(entity);
        });
    }

    @Override
    public TextureRegion getRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        return req.config == Boolean.TRUE ? openRegion : region;
    }

    public class DoorEntity extends Building{
        public boolean open = false;

        @Override
        public void draw(){
            Draw.rect(open ? openRegion : region, x, y);
        }

        @Override
        public Cursor getCursor(){
            return SystemCursor.hand;
        }

        @Override
        public boolean checkSolid(){
            return !open;
        }

        @Override
        public void tapped(Player player){
            if((Units.anyEntities(tile) && open) || !timer(timerToggle, 30f)){
                return;
            }

            configure(!open);
        }

        @Override
        public Boolean config(){
            return open;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(open);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            open = read.bool();
        }
    }

}
