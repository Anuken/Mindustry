package io.anuke.mindustry.content.fx;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;

public abstract class FxList implements ContentList{

    @Override
    public ContentType type(){
        return ContentType.effect;
    }
}
