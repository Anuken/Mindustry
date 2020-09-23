package mindustry.world.blocks;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

import java.util.*;

//TODO documentation
public interface Autotiler{

    //holds some static temporary variables, required due to some RoboVM bugs
    class AutotilerHolder{
        static final int[] blendresult = new int[5];
        static final BuildPlan[] directionals = new BuildPlan[4];
    }

    /** slices a texture region:
     * mode == 0 -> no slice
     * mode == 1 -> bottom
     * mode == 2 -> top */
    default TextureRegion sliced(TextureRegion input, int mode){
        return mode == 0 ? input : mode == 1 ? botHalf(input) : topHalf(input);
    }

    default TextureRegion topHalf(TextureRegion input){
        TextureRegion region = Tmp.tr1;
        region.set(input);
        region.setWidth(region.width / 2);
        return region;
    }

    default TextureRegion botHalf(TextureRegion input){
        TextureRegion region = Tmp.tr1;
        region.set(input);
        int width = region.width;
        region.setWidth(width / 2);
        region.setX(region.getX() + width);
        return region;
    }

    default @Nullable int[] getTiling(BuildPlan req, Eachable<BuildPlan> list){
        if(req.tile() == null) return null;
        BuildPlan[] directionals = AutotilerHolder.directionals;

        Arrays.fill(directionals, null);
        list.each(other -> {
            if(other.breaking || other == req) return;

            int i = 0;
            for(Point2 point : Geometry.d4){
                int x = req.x + point.x, y = req.y + point.y;
                if(x >= other.x -(other.block.size - 1) / 2 && x <= other.x + (other.block.size / 2) && y >= other.y -(other.block.size - 1) / 2 && y <= other.y + (other.block.size / 2)){
                    directionals[i] = other;
                }
                i++;
            }
        });

        return buildBlending(req.tile(), req.rotation, directionals, req.worldContext);
    }

    /**
     * @return an array of blending values:
     * [0]: the type of connection:
     *   - 0: straight
     *   - 1: curve (top)
     *   - 2: straight (bottom)
     *   - 3: all sides
     *   - 4: straight (top)
     * [1]: X scale
     * [2]: Y scale
     * [3]: a 4-bit mask with bits 0-3 indicating blend state in that direction (0 being 0 degrees, 1 being 90, etc)
     * [4]: same as [3] but only blends with non-square sprites
     * */
    default int[] buildBlending(Tile tile, int rotation, BuildPlan[] directional, boolean world){
        int[] blendresult = AutotilerHolder.blendresult;
        blendresult[0] = 0;
        blendresult[1] = blendresult[2] = 1;
        int num =
        (blends(tile, rotation, directional, 2, world) && blends(tile, rotation, directional, 1, world) && blends(tile, rotation, directional, 3, world)) ? 0 :
        (blends(tile, rotation, directional, 1, world) && blends(tile, rotation, directional, 3, world)) ? 1 :
        (blends(tile, rotation, directional, 1, world) && blends(tile, rotation, directional, 2, world)) ? 2 :
        (blends(tile, rotation, directional, 3, world) && blends(tile, rotation, directional, 2, world)) ? 3 :
        blends(tile, rotation, directional, 1, world) ? 4 :
        blends(tile, rotation, directional, 3, world) ? 5 :
        -1;
        transformCase(num, blendresult);

        blendresult[3] = 0;

        for(int i = 0; i < 4; i++){
            if(blends(tile, rotation, directional, i, world)){
                blendresult[3] |= (1 << i);
            }
        }

        blendresult[4] = 0;

        for(int i = 0; i < 4; i++){
            int realDir = Mathf.mod(rotation - i, 4);
            if(blends(tile, rotation, directional, i, world) && (tile != null && tile.getNearbyEntity(realDir) != null && !tile.getNearbyEntity(realDir).block.squareSprite)){
                blendresult[4] |= (1 << i);
            }
        }

        return blendresult;
    }

    default void transformCase(int num, int[] bits){
        if(num == 0){
            bits[0] = 3;
        }else if(num == 1){
            bits[0] = 4;
        }else if(num == 2){
            bits[0] = 2;
        }else if(num == 3){
            bits[0] = 2;
            bits[2] = -1;
        }else if(num == 4){
            bits[0] = 1;
            bits[2] = -1;
        }else if(num == 5){
            bits[0] = 1;
        }
    }

    default boolean facing(int x, int y, int rotation, int x2, int y2){
        return Point2.equals(x + Geometry.d4(rotation).x,y + Geometry.d4(rotation).y, x2, y2);
    }

    default boolean blends(Tile tile, int rotation, @Nullable BuildPlan[] directional, int direction, boolean checkWorld){
        int realDir = Mathf.mod(rotation - direction, 4);
        if(directional != null && directional[realDir] != null){
            BuildPlan req = directional[realDir];
            if(blends(tile, rotation, req.x, req.y, req.rotation, req.block)){
                return true;
            }
        }
        return checkWorld && blends(tile, rotation, direction);
    }

    default boolean blends(Tile tile, int rotation, int direction){
        Building other = tile.getNearbyEntity(Mathf.mod(rotation - direction, 4));
        return other != null && other.team == tile.team() && blends(tile, rotation, other.tileX(), other.tileY(), other.rotation, other.block);
    }

    default boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
                || ((!otherblock.rotatedOutput(otherx, othery) && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
                Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) || (otherblock.rotatedOutput(otherx, othery) && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y)));
    }

    /** @return whether this other block is *not* looking at this one. */
    default boolean notLookingAt(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return !(otherblock.rotatedOutput(otherx, othery) && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y));
    }

    /** @return whether this tile is looking at the other tile, or the other tile is looking at this one.
     * If the other tile does not rotate, it is always considered to be facing this one. */
    default boolean lookingAtEither(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
        || (!otherblock.rotatedOutput(otherx, othery) || Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y)));
    }

    /** @return whether this tile is looking at the other tile. */
    default boolean lookingAt(Tile tile, int rotation, int otherx, int othery, Block otherblock){
        Tile facing = Edges.getFacingEdge(otherblock, otherx, othery, tile);
        return facing != null &&
            Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, facing.x, facing.y);
    }

    boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock);
}
