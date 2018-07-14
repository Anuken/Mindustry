package io.anuke.mindustry.world.meta;

import io.anuke.mindustry.game.UnlockableContent;

public interface ContentStatValue extends StatValue{
    UnlockableContent[] getValueContent();
}
