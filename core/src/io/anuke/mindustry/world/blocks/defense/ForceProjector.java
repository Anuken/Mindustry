package io.anuke.mindustry.world.blocks.defense;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.entities.trait.DrawTrait;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.bulletGroup;

public class ForceProjector extends Block {

    public ForceProjector(String name) {
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void update(Tile tile){
        ForceEntity entity = tile.entity();

        if(entity.shield == null){
            entity.shield = new ShieldEntity(tile);
            entity.shield.add();
        }
    }

    @Override
    public TileEntity getEntity(){
        return new ForceEntity();
    }

    class ForceEntity extends TileEntity{
        ShieldEntity shield;
    }

    class ShieldEntity extends BaseEntity implements DrawTrait, SyncTrait{
        final Tile tile;
        final ForceProjector block;

        public ShieldEntity(Tile tile){
            this.tile = tile;
            this.block = (ForceProjector)tile.block();
        }

        @Override
        public void draw(){

        }

        @Override
        public EntityGroup targetGroup(){
            return bulletGroup;
        }

        @Override
        public boolean isSyncing(){
            return false;
        }

        @Override
        public void write(DataOutput data) throws IOException{}

        @Override
        public void read(DataInput data, long time) throws IOException{}
    }
}
