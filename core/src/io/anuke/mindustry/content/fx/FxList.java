package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;

public abstract class FxList implements ContentList{

    @Override
    public Array<? extends Content> getAll() {
        return Array.with();
    }
}
