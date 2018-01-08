package io.anuke.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;

public class KryoRegistrator {

    public static void register(Kryo kryo){
        kryo.register(Weapon.class, new Serializer<Weapon>() {
            @Override
            public void write(Kryo kryo, Output output, Weapon object) {
                output.writeByte(object.id);
            }

            @Override
            public Weapon read(Kryo kryo, Input input, Class type) {
                return (Weapon)Upgrade.getByID(input.readByte());
            }
        });

        kryo.register(Mech.class, new Serializer<Mech>() {
            @Override
            public void write(Kryo kryo, Output output, Mech object) {
                output.writeByte(object.id);
            }

            @Override
            public Mech read(Kryo kryo, Input input, Class type) {
                return (Mech)Upgrade.getByID(input.readByte());
            }
        });
    }
}
