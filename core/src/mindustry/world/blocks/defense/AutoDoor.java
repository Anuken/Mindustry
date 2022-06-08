package mindustry.world.blocks.defense;

import arc.audio.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class AutoDoor extends Wall{
    protected final static Rect rect = new Rect();
    protected final static Seq<Unit> units = new Seq<>();
    protected final static Boolf<Unit> groundCheck = u -> u.isGrounded() && !u.type.allowLegStep;

    public final int timerToggle = timers++;

    public float checkInterval = 20f;
    public Effect openfx = Fx.dooropen;
    public Effect closefx = Fx.doorclose;
    public Sound doorSound = Sounds.door;
    public @Load("@-open") TextureRegion openRegion;
    public float triggerMargin = 12f;

    public AutoDoor(String name){
        super(name);
        solid = false;
        solidifes = true;
        update = true;
        teamPassable = true;

        noUpdateDisabled = true;
        drawDisabled = true;
    }

    @Remote(called = Loc.server)
    public static void autoDoorToggle(Tile tile, boolean open){
        if(tile == null || !(tile.build instanceof AutoDoorBuild build)) return;
        build.setOpen(open);
    }

    public class AutoDoorBuild extends Building{
        public boolean open = false;

        public AutoDoorBuild(){
            //make sure it is staggered
            timer.reset(timerToggle, Mathf.random(checkInterval));
        }

        @Override
        public void updateTile(){
            if(timer(timerToggle, checkInterval) && !net.client()){
                units.clear();
                team.data().tree().intersect(rect.setSize(size * tilesize + triggerMargin * 2f).setCenter(x, y), units);
                boolean shouldOpen = units.contains(groundCheck);

                if(open != shouldOpen){
                    Call.autoDoorToggle(tile, shouldOpen);
                }
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.enabled) return open ? 1 : 0;
            return super.sense(sensor);
        }

        public void setOpen(boolean open){
            this.open = open;
            pathfinder.updateTile(tile);
            if(wasVisible){
                (!open ? closefx : openfx).at(this, size);
                doorSound.at(this);
            }
        }

        @Override
        public void draw(){
            Draw.rect(open ? openRegion : region, x, y);
        }

        @Override
        public boolean checkSolid(){
            return !open;
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
