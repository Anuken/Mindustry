package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.BasicBulletType;
import io.anuke.mindustry.entities.bullet.BulletType;

public class ShellBullets {
    public static final BulletType

    basicLeadShell = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    basicLeadShard = new BasicBulletType(3f, 0) {
        {
            drag = 0.1f;
            hiteffect = Fx.none;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
            bulletWidth = 9f;
            bulletHeight = 11f;
            bulletShrink = 1f;
        }
    },

    explosiveShell = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    explosiveShard = new BasicBulletType(3f, 0) {
        {
            drag = 0.1f;
            hiteffect = Fx.none;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
            bulletWidth = 9f;
            bulletHeight = 11f;
            bulletShrink = 1f;
        }
    },

    fragShell = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    fragShard = new BasicBulletType(3f, 0) {
        {
            drag = 0.1f;
            hiteffect = Fx.none;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
            bulletWidth = 9f;
            bulletHeight = 11f;
            bulletShrink = 1f;
        }
    },

    thoriumShell = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    thoriumShard = new BasicBulletType(3f, 0) {
        {
            drag = 0.1f;
            hiteffect = Fx.none;
            despawneffect = Fx.none;
            hitsize = 4;
            lifetime = 20f;
            bulletWidth = 9f;
            bulletHeight = 11f;
            bulletShrink = 1f;
        }
    },

    swarmMissile = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    scytheMissile = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    incendiaryMortar = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    },

    surgeMortar = new BasicBulletType(3f, 0) {
        {
            hiteffect = BulletFx.flakExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            drag = 0.01f;
            bulletWidth = bulletHeight = 9f;
            fragBullet = basicLeadShard;
            bulletSprite = "frag";
            bulletShrink = 0.1f;
        }
    };
}
