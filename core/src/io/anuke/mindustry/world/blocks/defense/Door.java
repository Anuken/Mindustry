package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.threads;

public class Door extends Wall{
    protected final Rectangle rect = new Rectangle();

    protected Effect openfx = BlockFx.dooropen;
    protected Effect closefx = BlockFx.doorclose;

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
        openRegion = Draw.region(name + "-open");
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
    public CursorType getCursor(Tile tile){
        return CursorType.hand;
    }

    @Override
    public boolean isSolidFor(Tile tile){
        DoorEntity entity = tile.entity();
        return !entity.open;
    }

    @Override
    public void tapped(Tile tile, Player player){
        DoorEntity entity = tile.entity();

        threads.run(() -> {

            if(Units.anyEntities(tile) && entity.open){
                return;
            }

            entity.open = !entity.open;
            if(!entity.open){
                Effects.effect(closefx, tile.drawx(), tile.drawy());
            }else{
                Effects.effect(openfx, tile.drawx(), tile.drawy());
            }
        });
    }

    @Override
    public TileEntity newEntity(){
        return new DoorEntity();
    }

    public class DoorEntity extends TileEntity{
        public boolean open = false;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeBoolean(open);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            open = stream.readBoolean();
        }
    }

}
