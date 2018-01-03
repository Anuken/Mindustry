package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.enemies.types.BlastEnemy;
import io.anuke.mindustry.entities.enemies.types.EmpEnemy;
import io.anuke.mindustry.entities.enemies.types.FastEnemy;
import io.anuke.mindustry.entities.enemies.types.FlamerEnemy;
import io.anuke.mindustry.entities.enemies.types.FortressEnemy;
import io.anuke.mindustry.entities.enemies.types.HealerEnemy;
import io.anuke.mindustry.entities.enemies.types.MortarEnemy;
import io.anuke.mindustry.entities.enemies.types.RapidEnemy;
import io.anuke.mindustry.entities.enemies.types.*;
import io.anuke.mindustry.entities.enemies.types.TankEnemy;
import io.anuke.mindustry.entities.enemies.types.TargetEnemy;
import io.anuke.mindustry.entities.enemies.types.TitanEnemy;

public class EnemyTypes {
    public static final EnemyType

    standard = new StandardEnemy(),
    fast = new FastEnemy(),
    rapid = new RapidEnemy(),
    flamer = new FlamerEnemy(),
    tank = new TankEnemy(),
    blast = new BlastEnemy(),
    mortar = new MortarEnemy(),
    healer = new HealerEnemy(),
    titan = new TitanEnemy(),
    emp = new EmpEnemy(),
    fortress = new FortressEnemy(),
    target = new TargetEnemy();

}
