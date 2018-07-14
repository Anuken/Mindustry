package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;

public abstract class BlockList implements ContentList{

    @Override
    public Array<? extends Content> getAll(){
        return Block.all();
    }
}
