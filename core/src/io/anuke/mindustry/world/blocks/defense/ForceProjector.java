package io.anuke.mindustry.world.blocks.defense;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.bulletGroup;

public class ForceProjector extends Block {

    public ForceProjector(String name) {
        super(name);
        update = true;
        solid = true;
        hasPower = true;
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
            set(tile.drawx(), tile.drawy());
        }

        @Override
        public void draw(){
            Draw.color(Palette.accent);
            Draw.alpha(0.5f);

            int range = 3;
            float rad = 12f;
            float space = rad*2-2f;
            for (int y = -range; y <= range; y++) {
                for (int x = -range; x <= range; x++) {
                    //if(Mathf.dst(x, y) > range) continue;
                    float wx = tile.drawx() + x * space + ((y + range) % 2)*space/2f;
                    float wy = tile.drawy() + y * (space-1);
                    Fill.poly(wx, wy, 6, rad);
                }
            }

            Draw.color();
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
