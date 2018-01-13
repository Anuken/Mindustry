package io.anuke.mindustry.net;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.entities.Entity;

public class Registrator {

    public static Class<?>[] getClasses(){
        return new Class<?>[]{
                StreamBegin.class,
                StreamChunk.class,
                WorldData.class,
                SyncPacket.class,
                PositionPacket.class,
                ShootPacket.class,
                PlacePacket.class,
                BreakPacket.class,
                StateSyncPacket.class,
                BlockSyncPacket.class,
                EnemySpawnPacket.class,
                BulletPacket.class,
                EnemyDeathPacket.class,
                BlockUpdatePacket.class,
                BlockDestroyPacket.class,
                ConnectPacket.class,
                DisconnectPacket.class,
                ChatPacket.class,
                KickPacket.class,
                UpgradePacket.class,
                WeaponSwitchPacket.class,
                BlockTapPacket.class,
                BlockConfigPacket.class,
                EntityRequestPacket.class,
                ConnectConfirmPacket.class,

                Class.class,
                byte[].class,
                float[].class,
                float[][].class,
                int[].class,
                int[][].class,
                Entity[].class,
                Array.class,
                Vector2.class,

                Entity.class,
                Player.class,
                Enemy.class
        };
    }
}
