package io.anuke.mindustry.world.modules;

import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.consumers.Consume;

import java.io.*;

public class ConsumeModule extends BlockModule{
    private boolean valid, optionalValid;
    private final TileEntity entity;

    public ConsumeModule(TileEntity entity){
        this.entity = entity;
    }

    public void update(){
        //everything is valid here
        if(entity.tile.isEnemyCheat()){
            valid = optionalValid = true;
            return;
        }

        boolean prevValid = valid();
        valid = true;
        optionalValid = true;
        boolean docons = entity.block.shouldConsume(entity.tile);

        for(Consume cons : entity.block.consumes.all()){
            if(cons.isOptional()) continue;

            if(docons && cons.isUpdate() && prevValid && cons.valid(entity)){
                cons.update(entity);
            }

            valid &= cons.valid(entity);
        }

        for(Consume cons : entity.block.consumes.optionals()){
            if(docons && cons.isUpdate() && prevValid && cons.valid(entity)){
                cons.update(entity);
            }

            optionalValid &= cons.valid(entity);
        }
    }

    public void trigger(){
        for(Consume cons : entity.block.consumes.all()){
            cons.trigger(entity);
        }
    }

    public boolean valid(){
        return valid && entity.block.canProduce(entity.tile);
    }

    public boolean optionalValid(){
        return valid() && optionalValid;
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeBoolean(valid);
    }

    @Override
    public void read(DataInput stream) throws IOException{
        valid = stream.readBoolean();
    }
}
