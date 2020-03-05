package mindustry.world.blocks.defense;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.pathfinder;

public class Door extends Wall{
    protected final static Rect rect = new Rect();

    public final int timerToggle = timers++;
    public Effect openfx = Fx.dooropen;
    public Effect closefx = Fx.doorclose;

    protected TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;
        entityType = DoorEntity::new;

        config(Boolean.class, (tile, open) -> {
            DoorEntity entity = tile.ent();
            entity.open = open;

            pathfinder.updateTile(tile);
            if(!entity.open){
                openfx.at(tile.drawx(), tile.drawy());
            }else{
                closefx.at(tile.drawx(), tile.drawy());
            }
            Sounds.door.at(tile);
        });
    }

    @Override
    public void load(){
        super.load();
        openRegion = Core.atlas.find(name + "-open");
    }

    @Override
    public void draw(Tile tile){
        DoorEntity entity = tile.ent();
        Draw.rect(entity.open ? openRegion : region, tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion getRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        return req.config == Boolean.TRUE ? openRegion : region;
    }

    @Override
    public Cursor getCursor(Tile tile){
        return SystemCursor.hand;
    }

    @Override
    public boolean isSolidFor(Tile tile){
        DoorEntity entity = tile.ent();
        return !entity.open;
    }

    @Override
    public void tapped(Tile tile, Playerc player){
        DoorEntity entity = tile.ent();

        if((Units.anyEntities(tile) && entity.open) || !tile.entity.timer(timerToggle, 30f)){
            return;
        }

        tile.configure(!entity.open);
    }

    public class DoorEntity extends TileEntity{
        public boolean open = false;

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
