package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.NuclearReactor;
import io.anuke.mindustry.world.meta.Attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeatedSmelter extends GenericSmelter {

    public HeatedSmelter(String name){
        super(name);
    }

    @Override
    public void onProximityUpdate(Tile tile){
        HeatedSmelterEntity entity = (HeatedSmelterEntity) tile.entity;
        super.onProximityUpdate(tile);

        entity.reactors.clear();
        entity.proximity().each(t -> {
            if(t.block() != null && t.block() instanceof NuclearReactor){
                entity.reactors.add(t);
            }
        });

    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        HeatedSmelterEntity entity = ((HeatedSmelterEntity) tile.entity);
        for(Tile reactor: ((HeatedSmelterEntity) tile.entity).reactors){
            NuclearReactor.NuclearReactorEntity e = (NuclearReactor.NuclearReactorEntity) reactor.entity;

            if(e.heat > entity.heat()){
                e.heat -= 0.01f;
                entity.AmbientHeat += 0.01f;
            }
        }

        if(entity.AmbientHeat > 0f){
            entity.AmbientHeat -= 0.001f;
        }
    }

    @Override
    protected float getProgressIncrease(TileEntity entity, float baseTime){
        return super.getProgressIncrease(entity, baseTime) * ((HeatedSmelterEntity)entity).heat();
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);

        HeatedSmelterEntity entity = tile.entity();
        entity.GroundHeat = sumAttribute(Attribute.heat, tile.x, tile.y) / 10;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", sumAttribute(Attribute.heat, x, y) * 10, 1), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("heat", entity -> new Bar("bar.heat", Pal.lightOrange, ((HeatedSmelterEntity) entity)::heat));
    }

    @Override
    public TileEntity newEntity(){
        return new HeatedSmelterEntity();
    }

    public static class HeatedSmelterEntity extends GenericCrafterEntity{
        public float GroundHeat = 0.0f;
        public float AmbientHeat = 0.0f;
        public Array<Tile> reactors = new Array<Tile>();

        public float heat(){
            return Mathf.clamp(GroundHeat + AmbientHeat);
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(GroundHeat);
            stream.writeFloat(AmbientHeat);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            GroundHeat = stream.readFloat();
            AmbientHeat = stream.readFloat();
        }
    }
}
