package mindustry.world;

import arc.math.*;
import arc.math.geom.*;
import mindustry.gen.*;

import java.util.*;

import static mindustry.Vars.world;

public class Edges{
    private static final int maxSize = 14;
    private static final int maxRadius = 12;
    private static Point2[][] edges = new Point2[maxSize][0];
    private static Point2[][] edgeInside = new Point2[maxSize][0];
    private static Vec2[][] polygons = new Vec2[maxRadius * 2][0];

    static{
        for(int i = 0; i < maxRadius * 2; i++){
            polygons[i] = Geometry.pixelCircle((i + 1) / 2f);
        }

        for(int i = 0; i < maxSize; i++){
            int bot = -(int)(i / 2f) - 1;
            int top = (int)(i / 2f + 0.5f) + 1;
            edges[i] = new Point2[(i + 1) * 4];

            int idx = 0;

            for(int j = 0; j < i + 1; j++){
                //bottom
                edges[i][idx++] = new Point2(bot + 1 + j, bot);
                //top
                edges[i][idx++] = new Point2(bot + 1 + j, top);
                //left
                edges[i][idx++] = new Point2(bot, bot + j + 1);
                //right
                edges[i][idx++] = new Point2(top, bot + j + 1);
            }

            Arrays.sort(edges[i], (e1, e2) -> Float.compare(Mathf.angle(e1.x, e1.y), Mathf.angle(e2.x, e2.y)));

            edgeInside[i] = new Point2[edges[i].length];

            for(int j = 0; j < edges[i].length; j++){
                Point2 point = edges[i][j];
                edgeInside[i][j] = new Point2(Mathf.clamp(point.x, -(int)((i) / 2f), (int)(i / 2f + 0.5f)),
                Mathf.clamp(point.y, -(int)((i) / 2f), (int)(i / 2f + 0.5f)));
            }
        }
    }

    public static Tile getFacingEdge(Building tile, Building other){
        return getFacingEdge(tile.block(), tile.tileX(), tile.tileY(), other.tile());
    }

    public static Tile getFacingEdge(Tile tile, Tile other){
        return getFacingEdge(tile.block, tile.x, tile.y, other);
    }

    public static Tile getFacingEdge(Block block, int tilex, int tiley, Tile other){
        if(!block.isMultiblock()) return world.tile(tilex, tiley);
        int size = block.size;
        return world.tile(tilex + Mathf.clamp(other.x - tilex, -(size - 1) / 2, (size / 2)),
         tiley + Mathf.clamp(other.y - tiley, -(size - 1) / 2, (size / 2)));
    }

    public static Vec2[] getPixelPolygon(float radius){
        if(radius < 1 || radius > maxRadius)
            throw new RuntimeException("Polygon size must be between 1 and " + maxRadius);
        return polygons[(int)(radius * 2) - 1];
    }

    public static Point2[] getEdges(int size){
        if(size < 0 || size > maxSize) throw new RuntimeException("Block size must be between 0 and " + maxSize);

        return edges[size - 1];
    }

    public static Point2[] getInsideEdges(int size){
        if(size < 0 || size > maxSize) throw new RuntimeException("Block size must be between 0 and " + maxSize);

        return edgeInside[size - 1];
    }
}
