package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.Core;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.Tile;

import java.io.*;

import static io.anuke.mindustry.Vars.world;

public class Door extends Wall{
    protected final Rectangle rect = new Rectangle();

    protected Effect openfx = Fx.dooropen;
    protected Effect closefx = Fx.doorclose;

    protected TextureRegion openRegion;

    public Door(String name){
        super(name);
        solid = false;
        solidifes = true;
        consumesTap = true;
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

        if(Units.anyEntities(tile) && entity.open){
            return;
        }

        entity.open = !entity.open;
        world.pathfinder.updateSolid(tile);
        if(!entity.open){
            Effects.effect(closefx, tile.drawx(), tile.drawy());
        }else{
            Effects.effect(openfx, tile.drawx(), tile.drawy());
        }
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
