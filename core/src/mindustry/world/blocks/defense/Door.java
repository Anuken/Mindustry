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

import mindustry.*;

import static mindustry.Vars.*;

public class Door extends Wall{
    protected final static Rect rect = new Rect();
    protected final static Queue<DoorBuild> doorQueue = new Queue<>();

    public Effect openfx = Fx.dooropen;
    public Effect closefx = Fx.doorclose;
    public Sound doorSound = Sounds.door;
    public boolean chainEffect = false;
    public @Load("@-open") TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;

        config(Boolean.class, (DoorBuild base, Boolean open) -> {
            if(!state.generating){
                doorSound.at(base);
                base.effect();
            }

            doorQueue.clear();
            doorQueue.add(base);

            for(DoorBuild entity : base.chained.isEmpty() ? doorQueue : base.chained){
                //skip doors with things in them
                if((Units.anyEntities(entity.tile) && !open) || entity.open == open){
                    continue;
                }

                if(chainEffect) entity.effect();
                entity.open = open;
                entity.recache();
                if(!state.generating) state.pathfinder.updateTile(entity.tile);
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
        public float lastToggleTime;

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
        public double sense(LogicProp sensor){
            if(sensor == LogicProp.enabled) return open ? 1 : 0;
            return super.sense(sensor);
        }

        @Override
        public void control(LogicExecutor executor, LogicProp type, double p1, double p2, double p3, double p4){
            if(type == LogicProp.enabled){
                boolean shouldOpen = !Mathf.zero(p1);

                if(net.client() || open == shouldOpen || (Units.anyEntities(tile) && !shouldOpen) || !origin().toggleReady(80f)){
                    return;
                }

                configureAny(shouldOpen);
            }
        }

        public boolean toggleReady(float cooldown){
            if(Vars.state.time - lastToggleTime < cooldown) return false;
            lastToggleTime = Vars.state.time;
            return true;
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
            if((Units.anyEntities(tile) && open) || !origin().toggleReady(60f)){
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
