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

    @Override
    public void search(Tile start, Tile end, Array<Tile> out){

    }

    public void search(Tile start, Predicate<Tile> result, Array<Tile> out){
        Queue<Tile> queue = new Queue<>();

        for(int i = 0; i < weights.length; i++){
            for(int j = 0; j < weights[0].length; j++){
                if(result.test(tiles[i][j])){
                    weights[i][j] = 100000;
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
                if(inBounds(nx, ny) && weights[nx][ny] < weights[tile.x][tile.y] - 1f && tiles[nx][ny].passable()){
                    weights[nx][ny] = weights[tile.x][tile.y] - 1;
                    queue.addLast(tiles[nx][ny]);
                    if(result.test(tiles[nx][ny])){
                        break;
                    }
                }
            }
        }

        out.add(start);
        while(true){
            Tile tile = out.peek();

            Tile max = null;
            float maxf = weights[tile.x][tile.y];
            for(GridPoint2 point : Geometry.d4){
                int nx = tile.x + point.x, ny = tile.y + point.y;
                if(inBounds(nx, ny) && (weights[nx][ny] > maxf)){
                    max = tiles[nx][ny];
                    maxf = weights[nx][ny];

                    if(MathUtils.isEqual(maxf, 100000)){
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
