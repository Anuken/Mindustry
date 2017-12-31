package io.anuke.mindustry.net;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Packets.EntityDataPacket;
import io.anuke.mindustry.net.Packets.SyncPacket;
import io.anuke.mindustry.net.Packets.WorldData;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.ucore.entities.Entity;

public class Registrator {

    public static Class<?>[] getClasses(){
        return new Class<?>[]{
                StreamBegin.class,
                StreamChunk.class,
                WorldData.class,
                SyncPacket.class,
                EntityDataPacket.class,
                Class.class,
                byte[].class,
                Entity[].class,
                Player[].class,
                Array.class,
                Vector2.class,

                Entity.class,
                Player.class,
                Mech.class
        };
    }
}
