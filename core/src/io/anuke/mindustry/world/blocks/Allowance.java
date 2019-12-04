package io.anuke.mindustry.world.blocks;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

/**
 * DRY location for allowance checks.
 *
 * fixme: include blocks that are under construction in the sum method
 */
public class Allowance{

    /**
     * Get how many of this block that team has.
     */
    public static int sum(Team team, Block block){
        return indexer.getAllied(team, BlockFlag.allied).select(tile -> tile.block() == block).size;
    }

    /**
     * Get the maximum amount this team can place that block.
     *
     *  0 = banned
     * -1 = infinite
     * ## = allowance
     */
    public static int max(Team team, Block block){
        if(state.rules.bannedBlocks.contains(block)) return 0; // todo: someday remove backwards compatibility

        if(!state.rules.allowance.containsKey(block)) return -1;

        return state.rules.allowance.get(block);
    }


    /**
     * Check if their allowance is spent
     *
     * (it happened at multiple places so i might as well pull it into here)
     *
     * true: ban placement
     * false: allow placement
     */
    public static boolean spent(Team team, Block block){
        return Allowance.max(team, block) != -1 && Allowance.sum(team, block) >= Allowance.max(team, block);
    }

    /**
     * Ensure all blocks are indexed for the above sum method.
     *
     * (if you have a better idea how to do it i'm all ears)
     */
    public static void flag(Block block){
        if(block.flags.contains(BlockFlag.allied)) return;

        Array<BlockFlag> tmp = new Array<>();
        for(BlockFlag flag : block.flags){
            tmp.add(flag);
        }

        tmp.add(BlockFlag.allied);
        block.flags = EnumSet.of(tmp.toArray(BlockFlag.class));
    }
}
