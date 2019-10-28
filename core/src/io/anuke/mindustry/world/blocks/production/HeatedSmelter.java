package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.meta.Attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeatedSmelter extends GenericSmelter {

    public HeatedSmelter(String name){
        super(name);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", sumAttribute(Attribute.heat, x, y) * 10, 1), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("heat", entity -> new Bar("bar.heat", Pal.lightOrange, () -> ((HeatedSmelterEntity)entity).heat));
    }

    @Override
    public TileEntity newEntity() {
        return new HeatedSmelterEntity();
    }

    public static class HeatedSmelterEntity extends GenericCrafterEntity {
        public float heat;

        @Override
        public void write(DataOutput stream) throws IOException {
            super.write(stream);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            heat = stream.readFloat();
        }
    }
}
