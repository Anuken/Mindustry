package io.anuke.mindustry.resource;

import io.anuke.mindustry.entities.bullets.BulletType;

public abstract class AmmoType {

    public abstract BulletType getBullet(Item item);
}
