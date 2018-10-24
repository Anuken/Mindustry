package io.anuke.mindustry.content.bullets;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;

public abstract class BulletList implements ContentList{

    @Override
    public ContentType type(){
        return ContentType.bullet;
    }
}
