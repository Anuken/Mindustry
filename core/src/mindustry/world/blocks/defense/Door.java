package mindustry.world.blocks.defense;

import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class Door extends Wall{
    protected final static Rect rect = new Rect();
    protected final static Queue<DoorBuild> doorQueue = new Queue<>();

    public final int timerToggle = timers++;
    public Effect openfx = Fx.dooropen;
    public Effect closefx = Fx.doorclose;
    public Sound doorSound = Sounds.door;
    public @Load("@-open") TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;

        config(Boolean.class, (DoorBuild base, Boolean open) -> {
            doorSound.at(base);
            base.effect();

            for(DoorBuild entity : base.chained){
                //skip doors with things in them
                if((Units.anyEntities(entity.tile) && !open) || entity.open == open){
                    continue;
                }

                entity.open = open;
                pathfinder.updateTile(entity.tile());
            }
        });
    }

    @Override
    public TextureRegion getPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        return plan.config == Boolean.TRUE ? openRegion : region;
    }

    public class DoorBuild extends Building{
        public boolean open = false;
        public Seq<DoorBuild> chained = new Seq<>();

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();
            updateChained();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            for(Building b : proximity){
                if(b instanceof DoorBuild d){
                    d.updateChained();
                }
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.enabled) return open ? 1 : 0;
            return super.sense(sensor);
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            if(type == LAccess.enabled){
                boolean shouldOpen = !Mathf.zero(p1);

                if(net.client() || open == shouldOpen || (Units.anyEntities(tile) && !shouldOpen) || !origin().timer(timerToggle, 80f)){
                    return;
                }

                configureAny(shouldOpen);
            }
        }

        public DoorBuild origin(){
            return chained.isEmpty() ? this : chained.first();
        }

        public void effect(){
            (open ? closefx : openfx).at(this, size);
        }

        public void updateChained(){
            chained = new Seq<>();
            doorQueue.clear();
            doorQueue.add(this);

            while(!doorQueue.isEmpty()){
                var next = doorQueue.removeLast();
                chained.add(next);

                for(var b : next.proximity){
                    if(b instanceof DoorBuild d && d.chained != chained){
                        d.chained = chained;
                        doorQueue.addFirst(d);
                    }
                }
            }
        }

        @Override
        public void draw(){
            Draw.rect(open ? openRegion : region, x, y);
        }

        @Override
        public Cursor getCursor(){
            return interactable(player.team()) ? SystemCursor.hand : SystemCursor.arrow;
        }

        @Override
        public boolean checkSolid(){
            return !open;
        }

        @Override
        public void tapped(){
            if((Units.anyEntities(tile) && open) || !origin().timer(timerToggle, 60f)){
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
