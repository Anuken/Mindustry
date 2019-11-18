package io.anuke.mindustry.entities.bullet;

import io.anuke.arc.util.Log;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.type.UnitType;
import io.anuke.mindustry.world.Tile;

public class ConvertBulletType extends LiquidBulletType {
    public ConvertBulletType() {
        super(Liquids.water);
    }

    @Override
    public void hitTile(Bullet b, Tile tile) {
        tile.setTeam(b.getTeam());
    }

    @Override
    public void hitUnit(Bullet b, Unit unit) {
        super.hitUnit(b, unit);

        unit.setTeam(b.getTeam());
    }
}
