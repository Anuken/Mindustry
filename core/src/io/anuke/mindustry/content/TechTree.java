package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.type.ItemStack.with;

public class TechTree implements ContentList{
    public static TechNode root;

    @Override
    public void load(){
        root = new TechNode(null, with(),
            new TechNode(Blocks.copperWall, with(Items.copper, 100),
                new TechNode(Blocks.copperWallLarge, with(Items.copper, 100))
            ),

            new TechNode(Blocks.conveyor, with(Items.copper, 10),
                new TechNode(Blocks.router, with(Items.copper, 10),
                    new TechNode(Blocks.distributor, with(Items.copper, 10)),
                    new TechNode(Blocks.overflowGate, with(Items.copper, 10)),
                    new TechNode(Blocks.sorter, with(Items.copper, 10))
                ),
                new TechNode(Blocks.junction, with(Items.copper, 10)),
                new TechNode(Blocks.itemBridge, with(Items.copper, 10))
            ),

            new TechNode(Blocks.conduit, with(Items.metaglass, 10),
                new TechNode(Blocks.liquidJunction, with(Items.metaglass, 10)),
                new TechNode(Blocks.liquidRouter, with(Items.metaglass, 10),
                new TechNode(Blocks.liquidTank, with(Items.metaglass, 10))
                )
            )
        );
    }

    public static class TechNode{
        public final Block block;
        public final ItemStack[] requirements;
        public final TechNode[] children;
        public TechNode parent;

        TechNode(Block block, ItemStack[] requirements, TechNode... children){
            this.block = block;
            this.requirements = requirements;
            this.children = children;
            for(TechNode node : children){
                node.parent = this;
            }
        }
    }
}
