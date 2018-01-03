package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.enemies.types.BlastType;
import io.anuke.mindustry.entities.enemies.types.EmpType;
import io.anuke.mindustry.entities.enemies.types.FastType;
import io.anuke.mindustry.entities.enemies.types.FlamerType;
import io.anuke.mindustry.entities.enemies.types.FortressType;
import io.anuke.mindustry.entities.enemies.types.HealerType;
import io.anuke.mindustry.entities.enemies.types.MortarType;
import io.anuke.mindustry.entities.enemies.types.RapidType;
import io.anuke.mindustry.entities.enemies.types.*;
import io.anuke.mindustry.entities.enemies.types.TankType;
import io.anuke.mindustry.entities.enemies.types.TargetType;
import io.anuke.mindustry.entities.enemies.types.TitanType;

public class EnemyTypes {
    public static final EnemyType

    standard = new StandardType(),
    fast = new FastType(),
    rapid = new RapidType(),
    flamer = new FlamerType(),
    tank = new TankType(),
    blast = new BlastType(),
    mortar = new MortarType(),
    healer = new HealerType(),
    titan = new TitanType(),
    emp = new EmpType(),
    fortress = new FortressType(),
    target = new TargetType();

}
