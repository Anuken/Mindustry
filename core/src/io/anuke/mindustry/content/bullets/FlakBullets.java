package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.FlakBulletType;
import io.anuke.mindustry.type.ContentList;

public class FlakBullets extends BulletList implements ContentList{
    public static BulletType plastic, explosive, surge;

    @Override
    public void load(){


        plastic = new FlakBulletType(4f, 5){
            {

            }
        };

        explosive = new FlakBulletType(4f, 5){
            {

            }
        };

        surge = new FlakBulletType(4f, 5){
            {

            }
        };
    }
}
