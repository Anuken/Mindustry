package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.storage.Vault;

public class StorageBlocks extends BlockList implements ContentList{
    public static Block core, vault, unloader;

    @Override
    public void load(){
        core = new CoreBlock("core"){{
            health = 800;
        }};

        vault = new Vault("vault"){{
            size = 3;
            health = 600;
            itemCapacity = 2000;
        }};

        unloader = new SortedUnloader("unloader"){{
            speed = 5;
        }};
    }
}
