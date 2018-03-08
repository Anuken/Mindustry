package io.anuke.mindustry.world.blocks.types.modules;

import io.anuke.mindustry.world.blocks.types.BlockModule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PowerModule extends BlockModule{
    public float amount;
    public float capacity = 10f;
    public float voltage = 0.0001f;

    public boolean acceptsPower(){
        return amount + 0.001f <= capacity;
    }

    public float addPower(float add){
        if(add < voltage){
            return add;
        }

        float canAccept = Math.min(capacity - amount, add);

        amount += canAccept;

        return canAccept;
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        stream.writeFloat(amount);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        amount = stream.readFloat();
    }
}
