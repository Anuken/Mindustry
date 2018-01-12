package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Heuristic;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.mindustry.world.blocks.types.production.Drill;
import io.anuke.mindustry.world.blocks.types.production.Generator;
import io.anuke.mindustry.world.blocks.types.production.Pump;
import io.anuke.mindustry.world.blocks.types.production.Smelter;
import io.anuke.ucore.function.Predicate;

public class Heuristics {
    /**How many times more it costs to go through a destructible block than an empty block.*/
    static final float solidMultiplier = 5f;
    /**How many times more it costs to go through a tile that touches a solid block.*/
    static final float occludedMultiplier = 5f;

    public static class FastestHeuristic implements Heuristic<Tile> {

        @Override
        public float estimate(Tile node, Tile other){
            //Get Manhattan distance cost
            float cost = Math.abs(node.worldx() - other.worldx()) + Math.abs(node.worldy() - other.worldy());

            //If either one of the tiles is a breakable solid block (that is, it's player-made),
            //increase the cost by the tilesize times the solid block multiplier
            //Also add the block health, so blocks with more health cost more to traverse
            if(node.breakable() && node.block().solid) cost += Vars.tilesize* solidMultiplier + node.block().health;
            if(other.breakable() && other.block().solid) cost += Vars.tilesize* solidMultiplier + other.block().health;

            //if this block has solid blocks near it, increase the cost, as we don't want enemies hugging walls
            if(node.occluded) cost += Vars.tilesize*occludedMultiplier;

            return cost;
        }
    }

    public static class DestrutiveHeuristic implements Heuristic<Tile> {
        private final Predicate<Block> frees;

        public DestrutiveHeuristic(Predicate<Block> frees){
            this.frees = frees;
        }

        @Override
        public float estimate(Tile node, Tile other){
            //Get Manhattan distance cost
            float cost = Math.abs(node.worldx() - other.worldx()) + Math.abs(node.worldy() - other.worldy());

            //If either one of the tiles is a breakable solid block (that is, it's player-made),
            //increase the cost by the tilesize times the solid block multiplier
            //Also add the block health, so blocks with more health cost more to traverse
            if(node.breakable() && node.block().solid) cost += Vars.tilesize* solidMultiplier + node.block().health;
            if(other.breakable() && other.block().solid) cost += Vars.tilesize* solidMultiplier + other.block().health;

            //if this block has solid blocks near it, increase the cost, as we don't want enemies hugging walls
            if(node.occluded) cost += Vars.tilesize*occludedMultiplier;

            if(other.getLinked() != null) other = other.getLinked();
            if(node.getLinked() != null) node = node.getLinked();

            //generators are free!
            if(frees.test(other.block()) || frees.test(node.block())) cost = 0;

            return cost;
        }

        private boolean generator(Tile tile){
            return tile.block() instanceof Generator || tile.block() instanceof Turret
                    || tile.block() instanceof Pump || tile.block() instanceof Drill || tile.block() instanceof Smelter;
        }
    }
}
