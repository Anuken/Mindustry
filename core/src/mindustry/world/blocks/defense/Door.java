package mindustry.world.blocks.defense;

import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

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

        config(Boolean.class, (DoorEntity base, Boolean open) -> {
            Sounds.door.at(base);

            for(DoorEntity entity : base.chained){
                entity.open = open;
                pathfinder.updateTile(entity.tile());
                entity.effect();
            }
        });
    }

    @Override
    public TextureRegion getRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        return req.config == Boolean.TRUE ? openRegion : region;
    }

    public class DoorEntity extends Building{
        public boolean open = false;
        public ObjectSet<DoorEntity> chained = new ObjectSet<>();

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();
            updateChained();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            for(Building b : proximity){
                if(b instanceof DoorEntity){
                    ((DoorEntity)b).updateChained();
                }
            }
        }

        public void effect(){
            (open ? closefx : openfx).at(this);
        }

        public void updateChained(){
            chained = new ObjectSet<>();
            flow(chained);
        }

        public void flow(ObjectSet<DoorEntity> set){
            if(!set.add(this)) return;

            this.chained = set;

            for(Building b : proximity){
                if(b instanceof DoorEntity){
                    ((DoorEntity)b).flow(set);
                }
            }
        }

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
