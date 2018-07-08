package io.anuke.mindustry.world.modules;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.consumers.Consume;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ConsumeModule extends BlockModule{
    private Array<Consume> consumers = new Array<>();
    private boolean valid;

    public void update(TileEntity entity){
        boolean prevValid = valid;
        valid = true;

        for(Consume cons : consumers){
            if(prevValid && entity.tile.block().shouldConsume(entity.tile)){
                cons.update(entity.getTile().block(), entity);
            }

            if(!cons.isOptional()){
                valid &= cons.valid(entity.getTile().block(), entity);
            }
        }
    }

    public boolean valid(){
        return valid;
    }

    public Array<Consume> all() {
        return consumers;
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        for(Consume cons : consumers){
            cons.write(stream);
        }
    }

    @Override
    public void read(DataInput stream) throws IOException {
        for(Consume cons : consumers){
            cons.read(stream);
        }
    }
}
