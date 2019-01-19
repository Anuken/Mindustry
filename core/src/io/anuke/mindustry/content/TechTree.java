package io.anuke.mindustry.content;

import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;

import static io.anuke.mindustry.type.ItemStack.with;

public class TechTree implements ContentList{
    public static TechNode root;

    @Override
    public void load(){
        root = new TechNode(Items.copper, with(),
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

            new TechNode(Items.lead, with(),
                new TechNode(Items.metaglass, with(),
                    new TechNode(Blocks.conduit, with(Items.metaglass, 10)),
                    new TechNode(Blocks.liquidJunction, with(Items.metaglass, 10)),
                    new TechNode(Blocks.liquidRouter, with(Items.metaglass, 10),
                        new TechNode(Blocks.liquidTank, with(Items.metaglass, 10))
                    )
                )
            )
        );
    }

    public static class TechNode{
        public final Content content;
        public final ItemStack[] requirements;
        public final TechNode[] children;
        public TechNode parent;

        TechNode(Content content, ItemStack[] requirements, TechNode... children){
            this.content = content;
            this.requirements = requirements;
            this.children = children;
            for(TechNode node : children){
                node.parent = this;
            }
        }
    }
}
