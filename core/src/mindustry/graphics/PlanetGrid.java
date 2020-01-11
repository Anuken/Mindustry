package mindustry.graphics;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

public class PlanetGrid{
    private static final float x = -0.525731112119133606f;
    private static final float z = -0.850650808352039932f;

    private static final Vec3[] iTiles = {
    new Vec3(-x, 0, z), new Vec3(x, 0, z), new Vec3(-x, 0, -z), new Vec3(x, 0, -z),
    new Vec3(0, z, x), new Vec3(0, z, -x), new Vec3(0, -z, x), new Vec3(0, -z, -x),
    new Vec3(z, x, 0), new Vec3(-z, x, 0), new Vec3(z, -x, 0), new Vec3(-z, -x, 0)
    };

    private static final  int[][] iTilesP = {
    {9, 4, 1, 6, 11}, {4, 8, 10, 6, 0}, {11, 7, 3, 5, 9}, {2, 7, 10, 8, 5},
    {9, 5, 8, 1, 0}, {2, 3, 8, 4, 9}, {0, 1, 10, 7, 11}, {11, 6, 10, 3, 2},
    {5, 3, 10, 1, 4}, {2, 5, 4, 0, 11}, {3, 7, 6, 1, 8}, {7, 2, 9, 0, 6}
    };

    Grid newGrid(int size){
        if(size == 0){
            return initialGrid();
        }else{
            return subdividedGrid(newGrid(size - 1));
        }
    }

    Grid initialGrid(){
        Grid grid = new Grid(0);

        for(Tile t : grid.tiles){
            t.v = iTiles[t.id];
            for(int k = 0; k < 5; k++){
                t.tiles[k] = grid.tiles[iTilesP[t.id][k]];
            }
        }
        for(int i = 0; i < 5; i++){
            addCorner(i, grid, 0, iTilesP[0][(i + 4) % 5], iTilesP[0][i]);
        }
        for(int i = 0; i < 5; i++){
            addCorner(i + 5, grid, 3, iTilesP[3][(i + 4) % 5], iTilesP[3][i]);
        }
        addCorner(10, grid, 10, 1, 8);
        addCorner(11, grid, 1, 10, 6);
        addCorner(12, grid, 6, 10, 7);
        addCorner(13, grid, 6, 7, 11);
        addCorner(14, grid, 11, 7, 2);
        addCorner(15, grid, 11, 2, 9);
        addCorner(16, grid, 9, 2, 5);
        addCorner(17, grid, 9, 5, 4);
        addCorner(18, grid, 4, 5, 8);
        addCorner(19, grid, 4, 8, 1);

        //add corners to corners
        for(Corner c : grid.corners){
            for(int k = 0; k < 3; k++){
                c.corners[k] = c.tiles[k].corners[(position(c.tiles[k], c) + 1) % 5];
            }
        }
        //new edges
        int nextEdge = 0;
        for(Tile t : grid.tiles){
            for(int k = 0; k < 5; k++){
                if(t.edges[k] == null){
                    addEdge(nextEdge, grid, t.id, iTilesP[t.id][k]);
                    nextEdge++;
                }
            }
        }
        return grid;
    }

    Grid subdividedGrid(Grid prev){
        Grid grid = new Grid(prev.size + 1);

        int prevTiles = prev.tiles.length;
        int prevCorners = prev.corners.length;

        //old tiles
        for(int i = 0; i < prevTiles; i++){
            grid.tiles[i].v = prev.tiles[i].v;
            for(int k = 0; k < grid.tiles[i].edgeCount; k++){

                grid.tiles[i].tiles[k] = grid.tiles[prev.tiles[i].corners[k].id + prevTiles];
            }
        }
        //old corners become tiles
        for(int i = 0; i < prevCorners; i++){
            grid.tiles[i + prevTiles].v = prev.corners[i].v;
            for(int k = 0; k < 3; k++){
                grid.tiles[i + prevTiles].tiles[2 * k] = grid.tiles[prev.corners[i].corners[k].id + prevTiles];
                grid.tiles[i + prevTiles].tiles[2 * k + 1] = grid.tiles[prev.corners[i].tiles[k].id];
            }
        }
        //new corners
        int nextCorner = 0;
        for(Tile n : prev.tiles){
            Tile t = grid.tiles[n.id];
            for(int k = 0; k < t.edgeCount; k++){
                addCorner(nextCorner, grid, t.id, t.tiles[(k + t.edgeCount - 1) % t.edgeCount].id, t.tiles[k].id);
                nextCorner++;
            }
        }
        //connect corners
        for(Corner c : grid.corners){
            for(int k = 0; k < 3; k++){
                c.corners[k] = c.tiles[k].corners[(position(c.tiles[k], c) + 1) % (c.tiles[k].edgeCount)];
            }
        }
        //new edges
        int nextEdge = 0;
        for(Tile t : grid.tiles){
            for(int k = 0; k < t.edgeCount; k++){
                if(t.edges[k] == null){
                    addEdge(nextEdge, grid, t.id, t.tiles[k].id);
                    nextEdge++;
                }
            }
        }

        return grid;
    }

    void addCorner(int id, Grid grid, int t1, int t2, int t3){
        Corner c = grid.corners[id];
        Tile[] t = {grid.tiles[t1], grid.tiles[t2], grid.tiles[t3]};
        c.v = Tmp.v31.set(t[0].v).add(t[1].v).add(t[2].v).cpy().nor();
        for(int i = 0; i < 3; i++){
            t[i].corners[position(t[i], t[(i + 2) % 3])] = c;
            c.tiles[i] = t[i];
        }
    }

    void addEdge(int id, Grid grid, int t1, int t2){
        Edge e = grid.edges[id];
        Tile[] t = {grid.tiles[t1], grid.tiles[t2]};
        Corner[] c = {
        grid.corners[t[0].corners[position(t[0], t[1])].id],
        grid.corners[t[0].corners[(position(t[0], t[1]) + 1) % t[0].edgeCount].id]};
        for(int i = 0; i < 2; i++){
            t[i].edges[position(t[i], t[(i + 1) % 2])] = e;
            e.tiles[i] = t[i];
            c[i].edges[position(c[i], c[(i + 1) % 2])] = e;
            e.corners[i] = c[i];
        }
    }

    int position(Tile t, Tile n){
        for(int i = 0; i < t.edgeCount; i++)
            if(t.tiles[i] == n)
                return i;
        return -1;
    }

    int position(Tile t, Corner c){
        for(int i = 0; i < t.edgeCount; i++)
            if(t.corners[i] == c)
                return i;
        return -1;
    }

    int position(Tile t, Edge e){
        for(int i = 0; i < t.edgeCount; i++)
            if(t.edges[i] == e)
                return i;
        return -1;
    }

    int position(Corner c, Corner n){
        for(int i = 0; i < 3; i++)
            if(c.corners[i] == n)
                return i;
        return -1;
    }

    static int tileCount(int size){
        return 10 * Mathf.pow(3, size) + 2;
    }

    static int cornerCount(int size){
        return 20 * Mathf.pow(3, size);
    }

    static int edgeCount(int size){
        return 30 * Mathf.pow(3, size);
    }

    static class Tile{
        int id;
        int edgeCount;
        Vec3 v = new Vec3();
        Tile[] tiles;
        Corner[] corners;
        Edge[] edges;

        public Tile(int id, int edgeCount){
            this.id = id;
            this.edgeCount = edgeCount;

            tiles = new Tile[edgeCount];
            corners = new Corner[edgeCount];
            edges = new Edge[edgeCount];
        }
    }

    static class Corner{
        int id;
        Tile[] tiles = new Tile[3];
        Corner[] corners = new Corner[3];
        Edge[] edges = new Edge[3];
        Vec3 v = new Vec3();

        public Corner(int id){
            this.id = id;
        }
    }

    static class Edge{
        int id;
        Tile[] tiles = new Tile[2];
        Corner[] corners = new Corner[2];

        public Edge(int id){
            this.id = id;
        }
    }

    static class Grid{
        int size;
        Tile[] tiles;
        Corner[] corners;
        Edge[] edges;

        Grid(int size){
            this.size = size;

            tiles = new Tile[tileCount(size)];
            for(int i = 0; i < tiles.length; i++){
                tiles[i] = new Tile(i, i < 12 ? 5 : 6);
            }

            corners = new Corner[cornerCount(size)];
            for(int i = 0; i < corners.length; i++){
                corners[i] = new Corner(i);
            }

            edges = new Edge[edgeCount(size)];
            for(int i = 0; i < edges.length; i++){
                edges[i] = new Edge(i);
            }
        }
    }
}
