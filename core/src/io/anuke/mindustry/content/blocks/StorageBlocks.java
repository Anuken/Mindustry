package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.storage.Vault;

public class StorageBlocks extends BlockList implements ContentList{
    public static Block core, vault, container, unloader;

    @Override
    public void load(){
        core = new CoreBlock("core"){{
            health = 1100;
            itemCapacity = 3000;
        }};

        vault = new Vault("vault"){{
            size = 3;
            itemCapacity = 1000;
        }};

        container = new Vault("container"){{
            size = 2;
            itemCapacity = 300;
        }};

        unloader = new SortedUnloader("unloader"){{
            speed = 7f;
        }};
    }
}
