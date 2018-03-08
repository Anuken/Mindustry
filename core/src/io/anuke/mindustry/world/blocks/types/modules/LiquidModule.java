package io.anuke.mindustry.world.blocks.types.modules;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.blocks.types.BlockModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LiquidModule extends BlockModule {
    public float amount;
    public Liquid liquid = Liquid.none;

    @Override
    public void write(DataOutputStream stream) throws IOException {
        stream.writeByte(liquid.id);
        stream.writeByte((byte)(amount));
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        byte id = stream.readByte();
        liquid = Liquid.getByID(id);
        amount = stream.readByte();
    }
}
