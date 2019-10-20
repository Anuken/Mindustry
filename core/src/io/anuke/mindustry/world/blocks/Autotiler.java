package io.anuke.mindustry.world.blocks;

import io.anuke.arc.function.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.world.*;

import java.util.*;

public interface Autotiler{
    class AutotilerHolder{
        static final int[] blendresult = new int[3];
        static final BuildRequest[] directionals = new BuildRequest[4];
    }

    default @Nullable int[] getTiling(BuildRequest req, Eachable<BuildRequest> list){
        if(req.tile() == null) return null;
        BuildRequest[] directionals = AutotilerHolder.directionals;

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

    default int[] buildBlending(Tile tile, int rotation, BuildRequest[] directional, boolean world){
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

    default boolean blends(Tile tile, int rotation, @Nullable BuildRequest[] directional, int direction, boolean checkWorld){
        int realDir = Mathf.mod(rotation - direction, 4);
        if(directional != null && directional[realDir] != null){
            BuildRequest req = directional[realDir];
            if(blends(tile, rotation, req.x, req.y, req.rotation, req.block)){
                return true;
            }
        }
        return checkWorld && blends(tile, rotation, direction);
    }

    default boolean blends(Tile tile, int rotation, int direction){
        Tile other = tile.getNearby(Mathf.mod(rotation - direction, 4));
        if(other != null) other = other.link();
        return other != null && blends(tile, rotation, other.x, other.y, other.rotation(), other.block());
    }

    default boolean lookingAt(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
        || (!otherblock.rotate || Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y)));
    }

    boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock);
}
