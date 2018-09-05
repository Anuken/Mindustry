package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.ContentList;

public abstract class BulletList implements ContentList{

    @Override
    public Array<? extends Content> getAll(){
        return BulletType.all();
    }
}
