package mindustry;

import arc.math.geom.Geometry;
import mindustry.content.Blocks;
import mindustry.world.Tile;

import static mindustry.Vars.world;

public class CliffSc {
    public void add() {
        for (Tile tile : world.tiles) {
            if (!tile.block().isStatic() || tile.block() == Blocks.cliff) continue;

            int rotation = 0;
            for (int i = 0; i < 8; i++) {
                Tile other = world.tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if (other != null && !other.block().isStatic()) {
                    rotation |= (1 << i);
                }
            }

            if (rotation != 0) {
                tile.setBlock(Blocks.cliff);
            }

            tile.data = (byte) rotation;
        }

        for (Tile tile : world.tiles) {
            if (tile.block() != Blocks.cliff && tile.block().isStatic()) {
                tile.setBlock(Blocks.air);
            }
        }
    }
}
