package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.types.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.types.storage.Unloader;
import io.anuke.mindustry.world.blocks.types.storage.Vault;

public class StorageBlocks {
    public static final Block

    core = new CoreBlock("core"){{
        health = 800;
    }},

    vault = new Vault("vault"){{
        size = 3;
    }},

    unloader = new Unloader("unloader"){{
        speed = 5;
    }},

    sortedunloader = new SortedUnloader("sortedunloader"){{
        speed = 5;
    }};
}
