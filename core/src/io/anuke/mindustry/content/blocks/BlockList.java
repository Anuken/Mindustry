package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;

public abstract class BlockList implements ContentList{

    @Override
    public ContentType type(){
        return ContentType.item;
    }
}
