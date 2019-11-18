package io.anuke.mindustry.entities.bullet;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.world.Tile;

public class ConvertBulletType extends LiquidBulletType {
    public ConvertBulletType() {
        super(Liquids.water);
    }

    @Override
    public void hitTile(Bullet b, Tile tile) {
        tile.setTeam(b.getTeam());
    }
}
