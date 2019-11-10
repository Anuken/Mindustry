package io.anuke.mindustry.io.versions;

import io.anuke.arc.func.Prov;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.base.*;

/*
Latest data: [build 81]

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

Before removal of lightining/bullet: [build 80]

0 = Player
1 = Fire
2 = Puddle
3 = Bullet
4 = Lightning
5 = Draug
6 = Spirit
7 = Phantom
8 = Dagger
9 = Crawler
10 = Titan
11 = Fortress
12 = Eruptor
13 = Wraith
14 = Ghoul
15 = Revenant

Before addition of new units: [build 79 and below]

0 = Player
1 = Fire
2 = Puddle
3 = Bullet
4 = Lightning
5 = Spirit
6 = Dagger
7 = Crawler
8 = Titan
9 = Fortress
10 = Eruptor
11 = Wraith
12 = Ghoul
13 = Phantom
14 = Revenant
 */
public class LegacyTypeTable{
    private static final Prov[] build81Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        Draug::new,
        Spirit::new,
        Phantom::new,
        Dagger::new,
        Crawler::new,
        Titan::new,
        Fortress::new,
        Eruptor::new,
        Wraith::new,
        Ghoul::new,
        Revenant::new
    };

    private static final Prov[] build80Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        Bullet::new, //TODO reading these may crash
        Lightning::new,
        Draug::new,
        Spirit::new,
        Phantom::new,
        Dagger::new,
        Crawler::new,
        Titan::new,
        Fortress::new,
        Eruptor::new,
        Wraith::new,
        Ghoul::new,
        Revenant::new
    };

    private static final Prov[] build79Table = {
        Player::new,
        Fire::new,
        Puddle::new,
        Bullet::new, //TODO reading these may crash
        Lightning::new,
        Spirit::new,
        Dagger::new,
        Crawler::new,
        Titan::new,
        Fortress::new,
        Eruptor::new,
        Wraith::new,
        Ghoul::new,
        Phantom::new,
        Revenant::new
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
