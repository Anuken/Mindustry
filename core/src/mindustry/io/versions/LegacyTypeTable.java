package mindustry.io.versions;

import arc.func.Prov;
import mindustry.entities.type.Bullet;
import mindustry.entities.effect.*;
import mindustry.entities.type.Player;
import mindustry.entities.type.base.*;

/*
Latest data: [build 81]

0 = Player
1 = Fire
2 = Puddle
3 = MinerDrone
4 = RepairDrone
5 = BuilderDrone
6 = GroundUnit
7 = GroundUnit
8 = GroundUnit
9 = GroundUnit
10 = GroundUnit
11 = FlyingUnit
12 = FlyingUnit
13 = Revenant

Before removal of lightining/bullet: [build 80]

0 = Player
1 = Fire
2 = Puddle
3 = Bullet
4 = Lightning
5 = MinerDrone
6 = RepairDrone
7 = BuilderDrone
8 = GroundUnit
9 = GroundUnit
10 = GroundUnit
11 = GroundUnit
12 = GroundUnit
13 = FlyingUnit
14 = FlyingUnit
15 = Revenant

Before addition of new units: [build 79 and below]

0 = Player
1 = Fire
2 = Puddle
3 = Bullet
4 = Lightning
5 = RepairDrone
6 = GroundUnit
7 = GroundUnit
8 = GroundUnit
9 = GroundUnit
10 = GroundUnit
11 = FlyingUnit
12 = FlyingUnit
13 = BuilderDrone
14 = Revenant
 */
public class LegacyTypeTable{
    /*
    0 = Player
1 = Fire
2 = Puddle
3 = Draug
4 = Spirit
5 = Phantom
6 = Dagger
7 = Crawler
8 = Titan
9 = Fortress
10 = Eruptor
11 = Wraith
12 = Ghoul
13 = Revenant
     */
    private static final Prov[] build81Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        MinerDrone::new,
        RepairDrone::new,
        BuilderDrone::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        FlyingUnit::new,
        FlyingUnit::new,
        HoverUnit::new
    };

    private static final Prov[] build80Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        Bullet::new,
        Lightning::new,
        MinerDrone::new,
        RepairDrone::new,
        BuilderDrone::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        FlyingUnit::new,
        FlyingUnit::new,
        HoverUnit::new
    };

    private static final Prov[] build79Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        Bullet::new,
        Lightning::new,
        RepairDrone::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        GroundUnit::new,
        FlyingUnit::new,
        FlyingUnit::new,
        BuilderDrone::new,
        HoverUnit::new
    };

    public static Prov[] getTable(int build){
        if(build == -1 || build == 81){
            //return most recent one since that's probably it; not guaranteed
            return build81Table;
        }else if(build == 80){
            return build80Table;
        }else{
            return build79Table;
        }
    }
}
