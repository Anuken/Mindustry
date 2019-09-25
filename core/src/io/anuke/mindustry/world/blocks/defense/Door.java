package io.anuke.mindustry.world.blocks.defense;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.Graphics.*;
import io.anuke.arc.Graphics.Cursor.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Door extends Wall{
    protected final Rectangle rect = new Rectangle();

    protected int timerToggle = timers++;
    protected Effect openfx = Fx.dooropen;
    protected Effect closefx = Fx.doorclose;

    protected TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;
    }

    @Remote(called = Loc.server)
    public static void onDoorToggle(Player player, Tile tile, boolean open){
        DoorEntity entity = tile.entity();
        if(entity != null){
            entity.open = open;
            Door door = (Door)tile.block();

            pathfinder.updateTile(tile);
            if(!entity.open){
                Effects.effect(door.openfx, tile.drawx(), tile.drawy());
            }else{
                Effects.effect(door.closefx, tile.drawx(), tile.drawy());
            }
            Sounds.door.at(tile);
        }
    }

    @Override
    public void load(){
        super.load();
        openRegion = Core.atlas.find(name + "-open");
    }

    @Override
    public void draw(Tile tile){
        DoorEntity entity = tile.entity();

        if(!entity.open){
            Draw.rect(region, tile.drawx(), tile.drawy());
        }else{
            Draw.rect(openRegion, tile.drawx(), tile.drawy());
        }
    }

    @Override
    public Cursor getCursor(Tile tile){
        return SystemCursor.hand;
    }

    @Override
    public boolean isSolidFor(Tile tile){
        DoorEntity entity = tile.entity();
        return !entity.open;
    }

    @Override
    public void tapped(Tile tile, Player player){
        DoorEntity entity = tile.entity();

        if((Units.anyEntities(tile) && entity.open) || !tile.entity.timer.get(timerToggle, 30f)){
            return;
        }

        Call.onDoorToggle(null, tile, !entity.open);
    }

    @Override
    public TileEntity newEntity(){
        return new DoorEntity();
    }

    public class DoorEntity extends TileEntity{
        public boolean open = false;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeBoolean(open);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            open = stream.readBoolean();
        }
    }

}
