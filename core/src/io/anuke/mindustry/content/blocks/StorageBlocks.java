package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.storage.Unloader;
import io.anuke.mindustry.world.blocks.storage.Vault;

public class StorageBlocks extends BlockList implements ContentList {
    public static Block core, vault, unloader, sortedunloader;

    @Override
    public void load() {
        core = new CoreBlock("core") {{
            health = 800;
        }};

        vault = new Vault("vault") {{
            size = 3;
            health = 600;
        }};

        unloader = new Unloader("unloader") {{
            speed = 5;
        }};

        sortedunloader = new SortedUnloader("sortedunloader") {{
            speed = 5;
        }};
    }
}
