package io.anuke.mindustry.world.blocks.modules;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.blocks.BlockModule;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class LiquidModule extends BlockModule {
    public float amount;
    /**Should never be null.*/
    public Liquid liquid = Liquids.none;

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeByte(liquid.id);
        stream.writeFloat(amount);
    }

    @Override
    public void read(DataInput stream) throws IOException{
        byte id = stream.readByte();
        liquid = Liquid.getByID(id);
        amount = stream.readFloat();
    }
}
