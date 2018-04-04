package io.anuke.mindustry.world.blocks.types.modules;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.blocks.types.BlockModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LiquidModule extends BlockModule {
    public float amount;
    /**Should never be null.*/
    public Liquid liquid = Liquids.none;

    @Override
    public void write(DataOutputStream stream) throws IOException {
        stream.writeByte(liquid.id);
        stream.writeFloat(amount);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        byte id = stream.readByte();
        liquid = Liquid.getByID(id);
        amount = stream.readFloat();
    }
}
