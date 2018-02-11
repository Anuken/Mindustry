package io.anuke.mindustry.server.mapgen;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.util.Geometry;

public class MapPathfinder {
    private IndexedGraph<Integer> graph = new MapGraph();
    private DefaultGraphPath<Integer> path = new DefaultGraphPath<>();
    private IndexedAStarPathFinder<Integer> finder = new IndexedAStarPathFinder<>(graph);
    private Heuristic<Integer> heuristic;
    private MapImage image;

    public Array<GridPoint2> find(int startx, int starty, int endx, int endy, Evaluator eval){
        finder.searchNodePath(image.pack(startx, starty), image.pack(endx, endy),
                (heuristic = (node, endNode) ->
                        eval.cost(image.get(node % image.width, node / image.width),
                                node % image.width,
                                node / image.width)), path);

        Array<GridPoint2> arr = new Array<>();

        for(int i : path.nodes){
            arr.add(new GridPoint2(i % image.width, i / image.width));
        }

        return arr;
    }

    private class MapGraph extends DefaultGraphPath<Integer> implements IndexedGraph<Integer>{
        private Array<Connection<Integer>> cons = new Array<>();

        @Override
        public int getIndex(Integer node) {
            return node;
        }

        @Override
        public int getNodeCount() {
            return image.width * image.height;
        }

        @Override
        public Array<Connection<Integer>> getConnections(Integer fromNode) {
            int x = fromNode % image.width;
            int y = fromNode / image.width;
            cons.clear();
            for(GridPoint2 p : Geometry.d4){
                if(image.has(x + p.x, y + p.y)){
                    cons.add(new MapConnection(fromNode, image.pack(x + p.x, y + p.y)));
                }
            }
            return cons;
        }
    }

    private class MapConnection implements Connection<Integer>{
        int from, to;

        MapConnection(int from, int to){
            this.from = from;
            this.to = to;
        }

        @Override
        public float getCost() {
            return heuristic.estimate(from, to);
        }

        @Override
        public Integer getFromNode() {
            return from;
        }

        @Override
        public Integer getToNode() {
            return to;
        }
    }

    interface Evaluator{
        int cost(Block block, int x, int y);
    }
}
