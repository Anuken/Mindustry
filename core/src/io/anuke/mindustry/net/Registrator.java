package io.anuke.mindustry.net;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.mindustry.resource.Mech;
import io.anuke.ucore.entities.Entity;

public class Registrator {

    public static Class<?>[] getClasses(){
        return new Class<?>[]{
                StreamBegin.class,
                StreamChunk.class,
                WorldData.class,
                SyncPacket.class,
                EntityDataPacket.class,
                PositionPacket.class,
                ShootPacket.class,
                PlacePacket.class,
                BreakPacket.class,
                StateSyncPacket.class,
                BlockSyncPacket.class,
                EnemySpawnPacket.class,
                PathPacket.class,
                BulletPacket.class,
                EnemyDeathPacket.class,
                BlockUpdatePacket.class,
                BlockDestroyPacket.class,

                Class.class,
                byte[].class,
                float[].class,
                float[][].class,
                int[].class,
                int[][].class,
                Entity[].class,
                Player[].class,
                Array.class,
                Vector2.class,

                Entity.class,
                Player.class,
                Mech.class,

                Enemy.class,
                FastEnemy.class,
                RapidEnemy.class,
                FlamerEnemy.class,
                TankEnemy.class,
                BlastEnemy.class,
                MortarEnemy.class,
                TestEnemy.class,
                HealerEnemy.class,
                TitanEnemy.class,
                EmpEnemy.class
        };
    }
}
