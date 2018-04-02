package io.anuke.mindustry.resource;

import io.anuke.mindustry.entities.bullets.BulletType;

public class AmmoType {
    public final Item item;
    public final BulletType bullet;
    public final int multiplier;

    public AmmoType(Item item, BulletType result, int multiplier){
        this.item = item;
        this.bullet = result;
        this.multiplier = multiplier;
    }
}
