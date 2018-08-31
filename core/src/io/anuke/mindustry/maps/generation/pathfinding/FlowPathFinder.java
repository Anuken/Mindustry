package io.anuke.mindustry.maps.generation.pathfinding;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.util.Geometry;

public class FlowPathFinder extends TilePathfinder{
    protected float[][] weights;

    public FlowPathFinder(Tile[][] tiles){
        super(tiles);
        this.weights = new float[tiles.length][tiles[0].length];
    }

    public void search(Tile start, Predicate<Tile> result, Array<Tile> out){
        Queue<Tile> queue = new Queue<>();

        for(int i = 0; i < weights.length; i++){
            for(int j = 0; j < weights[0].length; j++){
                if(result.test(tiles[i][j])){
                    weights[i][j] = Float.MAX_VALUE;
                    queue.addLast(tiles[i][j]);
                }else{
                    weights[i][j] = 0f;
                }
            }
        }

        while(queue.size > 0){
            Tile tile = queue.first();
            for(GridPoint2 point : Geometry.d4){
                int nx = tile.x + point.x, ny = tile.y + point.y;
                if(inBounds(nx, ny) && weights[nx][ny] < weights[tile.x][tile.y] && tiles[nx][ny].passable()){
                    weights[nx][ny] = weights[tile.x][tile.y] - 1;
                    queue.addLast(tiles[nx][ny]);
                }
            }
        }

        out.add(start);
        while(true){
            Tile tile = out.peek();

            Tile max = null;
            float maxf = 0f;
            for(GridPoint2 point : Geometry.d4){
                int nx = tile.x + point.x, ny = tile.y + point.y;
                if(inBounds(nx, ny) && (weights[nx][ny] > maxf || max == null)){
                    max = tiles[nx][ny];
                    maxf = weights[nx][ny];

                    if(MathUtils.isEqual(maxf, Float.MAX_VALUE)){
                        out.add(max);
                        return;
                    }
                }
            }
            if(max == null){
                break;
            }
            out.add(max);
        }
    }

}
