package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.PathFinderQueue;
import com.badlogic.gdx.ai.pfa.PathFinderRequest;
import com.badlogic.gdx.utils.BinaryHeap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

/**An IndexedAStarPathfinder that uses an OptimizedGraph, and therefore has less allocations.*/
public class OptimizedPathFinder {
    IntMap<NodeRecord> records = new IntMap<>();
    BinaryHeap<NodeRecord> openList;
    NodeRecord current;

    private int searchId;
    private Tile end;

    private static final byte UNVISITED = 0;
    private static final byte OPEN = 1;
    private static final byte CLOSED = 2;

    private static final boolean debug = false;

    public OptimizedPathFinder() {
        this.openList = new BinaryHeap<>();
    }

    public boolean searchNodePath(Tile startNode, Tile endNode, GraphPath<Tile> outPath) {
        this.end = endNode;

        // Perform AStar
        boolean found = search(startNode, endNode);

        if (found) {
            // Create a path made of nodes
            generateNodePath(startNode, outPath);
        }

        return found;
    }

    protected boolean search(Tile startNode, Tile endNode) {

        initSearch(startNode, endNode);

        // Iterate through processing each node
        do {
            // Retrieve the node with smallest estimated total cost from the open list
            current = openList.pop();
            current.category = CLOSED;

            // Terminate if we reached the goal node
            if (current.node == endNode) return true;

            visitChildren(endNode);

        } while (openList.size > 0);

        // We've run out of nodes without finding the goal, so there's no solution
        return false;
    }

    public boolean search(PathFinderRequest<Tile> request, long timeToRun) {

        long lastTime = TimeUtils.nanoTime();

        // We have to initialize the search if the status has just changed
        if (request.statusChanged) {
            initSearch(request.startNode, request.endNode);
            request.statusChanged = false;
        }

        // Iterate through processing each node
        do {

            // Check the available time
            long currentTime = TimeUtils.nanoTime();
            timeToRun -= currentTime - lastTime;
            if (timeToRun <= PathFinderQueue.TIME_TOLERANCE) return false;

            // Retrieve the node with smallest estimated total cost from the open list
            current = openList.pop();
            current.category = CLOSED;

            // Terminate if we reached the goal node; we've found a path.
            if (current.node == request.endNode) {
                request.pathFound = true;

                generateNodePath(request.startNode, request.resultPath);

                return true;
            }

            // Visit current node's children
            visitChildren(request.endNode);

            // Store the current time
            lastTime = currentTime;

        } while (openList.size > 0);

        // The open list is empty and we've not found a path.
        request.pathFound = false;
        return true;
    }

    protected void initSearch(Tile startNode, Tile endNode) {

        // Increment the search id
        if (++searchId < 0) searchId = 1;

        // Initialize the open list
        openList.clear();

        // Initialize the record for the start node and add it to the open list
        NodeRecord startRecord = getNodeRecord(startNode);
        startRecord.node = startNode;
        //startRecord.connection = null;
        startRecord.costSoFar = 0;
        addToOpenList(startRecord, estimate(startNode, endNode));

        current = null;
    }

    protected void visitChildren(Tile endNode) {
        if(debug) Effects.effect(Fx.node3, current.node.worldx(), current.node.worldy());

        nodes(current.node, node -> {
            float addCost = estimate(current.node, node);

            float nodeCost = current.costSoFar + addCost;

            float nodeHeuristic;
            NodeRecord nodeRecord = getNodeRecord(node);

            if (nodeRecord.category == CLOSED) { // The node is closed

                // If we didn't find a shorter route, skip
                if (nodeRecord.costSoFar <= nodeCost){
                    return;
                }

                // We can use the node's old cost values to calculate its heuristic
                // without calling the possibly expensive heuristic function
                nodeHeuristic = nodeRecord.getEstimatedTotalCost() - nodeRecord.costSoFar;
            } else if (nodeRecord.category == OPEN) { // The node is open

                //If our route is no better, then skip
                if (nodeRecord.costSoFar <= nodeCost){
                    return;
                }

                // Remove it from the open list (it will be re-added with the new cost)
                openList.remove(nodeRecord);

                // We can use the node's old cost values to calculate its heuristic
                // without calling the possibly expensive heuristic function
                nodeHeuristic = nodeRecord.getEstimatedTotalCost() - nodeRecord.costSoFar;
            } else { // the node is unvisited

                // We'll need to calculate the heuristic value using the function,
                // since we don't have a node record with a previously calculated value
                nodeHeuristic = estimate(node, endNode);
            }

            // Update node record's cost and connection
            nodeRecord.costSoFar = nodeCost;
            nodeRecord.from = current.node;

            // Add it to the open list with the estimated total cost
            addToOpenList(nodeRecord, nodeCost + nodeHeuristic);
        });
    }

    protected void nodes(Tile current, Consumer<Tile> cons){
        if(obstacle(current)) return;
        for(int i = 0; i < 4; i ++){
            Tile n = current.getNearby(i);
            if(!obstacle(n)) cons.accept(n);
        }
    }

    protected void jps(Tile current, int direction, Tile end, Consumer<Tile> cons){
        if(obstacle(current)) return; //skip solid or off-the-screen stuff

        //if there's no start point, scan everything.
        if(direction == -1){
            for(int i = 0; i < 8; i ++){
                jps(current.getNearby(Geometry.d8[i]), i, end, cons);
            }
            return;
        }

        if(direction % 2 == 0){
            //forced neighbor in the straight pattern
            if(obstacle(rel(current, direction + 2)) && !obstacle(rel(current, direction + 1))){
                cons.accept(rel(current, direction + 1));
            }

            if(obstacle(rel(current, direction - 2)) && !obstacle(rel(current, direction - 1))){
                cons.accept(rel(current, direction - 1));
            }
        }else{ //moving diagonal
            //forced neighbor in the diagonal pattern
            if(obstacle(rel(current, direction + 3)) && !obstacle(rel(current, direction + 2)) && !obstacle(rel(current, direction -2))) {
                cons.accept(rel(current, direction + 2));
            }

            if(obstacle(rel(current, direction - 3)) && !obstacle(rel(current, direction - 2))&& !obstacle(rel(current, direction + 2))){
                cons.accept(rel(current, direction - 2));
            }
        }

        while(!obstacle(current) && !trap(current, direction)){
            if(debug) Effects.effect(Fx.node1, current.worldx(), current.worldy());
            //moving straight
            if(direction % 2 == 0){
                Tile sf = scanDir(rel(current, direction), end, direction); //check if there's anything of interest going straight
                if(sf != null){ //if there is, jump to that location immediately and stop. else, nothing must be there, end.
                    cons.accept(sf);
                }
                return;
            }else{ //moving diagonal
                Tile sl = scanDir(rel(current, Mathf.mod(direction - 1, 8)), end, Mathf.mod(direction - 1, 8));

                if(sl != null){
                    cons.accept(sl);
                }

                Tile sr = scanDir(rel(current, Mathf.mod(direction + 1, 8)), end, Mathf.mod(direction + 1, 8));

                if(sr != null){
                    cons.accept(sr);
                }

                Tile sf = scanDir(rel(current, direction), end, direction);

                if(sf != null){
                    cons.accept(sf);
                    return;
                }
            }

            if(current == end){
                cons.accept(end);
                return;
            }

            current = rel(current, direction);
        }
    }

    protected boolean trap(Tile tile, int direction){
        return direction % 2 == 1 && obstacle(rel(tile, direction - 1)) && obstacle(rel(tile, direction + 1));
    }

    protected Tile scanDir(Tile tile, Tile end, int direction){
        while(!obstacle(tile)){
            if(debug) Effects.effect(Fx.node2, tile.worldx(), tile.worldy());
            if(tile == end) return tile;
            if(direction % 2 == 0){

                //forced neighbor in the straight pattern
                if((obstacle(rel(tile, direction + 2)) && !obstacle(rel(tile, direction + 1))) ||
                        (obstacle(rel(tile, direction - 2)) && !obstacle(rel(tile, direction - 1)))){
                    //Log.info("Found forced linear neighbor {0} {1} // {2}", tile.x, tile.y, direction);
                    if(debug) Effects.effect(Fx.node4, tile.worldx(), tile.worldy());
                    return tile;
                }
            }else{ //moving diagonal
                //forced neighbor in the diagonal pattern, end here
                if((obstacle(rel(tile, direction + 3)) && !obstacle(rel(tile, direction + 2))  && !obstacle(rel(tile, direction - 2))) ||
                        (obstacle(rel(tile, direction - 3)) && !obstacle(rel(tile, direction - 2)) && !obstacle(rel(tile, direction + 2)))) {
                    if(debug) Effects.effect(Fx.node4, tile.worldx(), tile.worldy());
                    //Log.info("Found forced diagonal neighbor {0} {1} // {2}", tile.x, tile.y, direction);
                    return tile;
                }else{
                    return null;
                }
            }
            Tile next = rel(tile, direction);
            if(obstacle(next)) break;
            tile = next;
        }
        return null;
    }

    protected Tile rel(Tile tile, int i){
        return tile.getNearby(Geometry.d8[Mathf.mod(i, 8)]);
    }

    protected boolean obstacle(Tile tile){
        return tile == null || (tile.solid() && end.target() != tile && tile.target() != end);
    }

    protected float estimate(Tile tile, Tile other){
        return Math.abs(tile.worldx() - other.worldx()) + Math.abs(tile.worldy() - other.worldy()) +0;
               // (tile.occluded ? tilesize : 0) + (other.occluded ? tilesize : 0);
    }

    protected int relDirection(Tile from, Tile current){
        if(from.y == current.y && from.x > current.x) return 0;
        if(from.y == current.y && from.x < current.x) return 4;
        if(from.x == current.x && from.y > current.y) return 2;
        if(from.x == current.x && from.y < current.y) return 6;

        if(from.y > current.y && from.x > current.x) return 1;
        if(from.y < current.y && from.x < current.x) return 5;
        if(from.x > current.x && from.y < current.y) return 7;
        if(from.x < current.x && from.y > current.y) return 3;
        return -1;
    }

    protected void generateNodePath(Tile startNode, GraphPath<Tile> outPath) {

        // Work back along the path, accumulating nodes
        // outPath.clear();
        while (current.from != null) {
            outPath.add(current.node);
            current = records.get(indexOf(current.from));
        }
        outPath.add(startNode);

        // Reverse the path
        outPath.reverse();
    }

    protected void addToOpenList(NodeRecord nodeRecord, float estimatedTotalCost) {
        openList.add(nodeRecord, estimatedTotalCost);
        nodeRecord.category = OPEN;
    }

    protected NodeRecord getNodeRecord(Tile node) {
        if(!records.containsKey(indexOf(node))){
            NodeRecord record = new NodeRecord();
            record.node = node;
            record.searchId = searchId;
            records.put(indexOf(node), record);
            return record;
        }else{
            NodeRecord record =  records.get(indexOf(node));
            if(record.searchId != searchId){
                record.category = UNVISITED;
                record.searchId = searchId;
            }
            return record;
        }
    }

    private int indexOf(Tile node){
        return node.packedPosition();
    }

    static class NodeRecord extends BinaryHeap.Node {
        Tile node;
        Tile from;

        float costSoFar;
        byte category;

        int searchId;

        public NodeRecord() {
            super(0);
        }

        public float getEstimatedTotalCost() {
            return getValue();
        }
    }
}
