package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.utils.BinaryHeap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;

/**An IndexedAStarPathfinder that uses an OptimizedGraph, and therefore has less allocations.*/
public class OptimizedPathFinder<N> implements PathFinder<N> {
    OptimizedGraph<N> graph;
    IntMap<NodeRecord<N>> records = new IntMap<>();
    BinaryHeap<NodeRecord<N>> openList;
    NodeRecord<N> current;

    /**
     * The unique ID for each search run. Used to mark nodes.
     */
    private int searchId;

    private static final byte UNVISITED = 0;
    private static final byte OPEN = 1;
    private static final byte CLOSED = 2;

    @SuppressWarnings("unchecked")
    public OptimizedPathFinder(OptimizedGraph<N> graph) {
        this.graph = graph;
        this.openList = new BinaryHeap<>();
    }

    @Override
    public boolean searchConnectionPath(N startNode, N endNode, Heuristic<N> heuristic, GraphPath<Connection<N>> outPath) {
        return false;
    }

    @Override
    public boolean searchNodePath(N startNode, N endNode, Heuristic<N> heuristic, GraphPath<N> outPath) {

        // Perform AStar
        boolean found = search(startNode, endNode, heuristic);

        if (found) {
            // Create a path made of nodes
            generateNodePath(startNode, outPath);
        }

        return found;
    }

    protected boolean search(N startNode, N endNode, Heuristic<N> heuristic) {

        initSearch(startNode, endNode, heuristic);

        // Iterate through processing each node
        do {
            // Retrieve the node with smallest estimated total cost from the open list
            current = openList.pop();
            current.category = CLOSED;

            // Terminate if we reached the goal node
            if (current.node == endNode) return true;

            visitChildren(endNode, heuristic);

        } while (openList.size > 0);

        // We've run out of nodes without finding the goal, so there's no solution
        return false;
    }

    @Override
    public boolean search(PathFinderRequest<N> request, long timeToRun) {

        long lastTime = TimeUtils.nanoTime();

        // We have to initialize the search if the status has just changed
        if (request.statusChanged) {
            initSearch(request.startNode, request.endNode, request.heuristic);
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
            visitChildren(request.endNode, request.heuristic);

            // Store the current time
            lastTime = currentTime;

        } while (openList.size > 0);

        // The open list is empty and we've not found a path.
        request.pathFound = false;
        return true;
    }

    protected void initSearch(N startNode, N endNode, Heuristic<N> heuristic) {

        // Increment the search id
        if (++searchId < 0) searchId = 1;

        // Initialize the open list
        openList.clear();

        // Initialize the record for the start node and add it to the open list
        NodeRecord<N> startRecord = getNodeRecord(startNode);
        startRecord.node = startNode;
        //startRecord.connection = null;
        startRecord.costSoFar = 0;
        addToOpenList(startRecord, heuristic.estimate(startNode, endNode));

        current = null;
    }

    protected void visitChildren(N endNode, Heuristic<N> heuristic) {
        // Get current node's outgoing connections
        //Array<Connection<N>> connections = graph.getConnections(current.node);
        N[] conn = graph.connectionsOf(current.node);

        // Loop through each connection in turn
        for (int i = 0; i < conn.length; i++) {

            //Connection<N> connection = connections.get(i)

            // Get the cost estimate for the node
            N node = conn[i];

            if(node == null) continue;

            float addCost = heuristic.estimate(current.node, node);

            float nodeCost = current.costSoFar + addCost;

            float nodeHeuristic;
            NodeRecord<N> nodeRecord = getNodeRecord(node);
            if (nodeRecord.category == CLOSED) { // The node is closed

                // If we didn't find a shorter route, skip
                if (nodeRecord.costSoFar <= nodeCost) continue;

                // We can use the node's old cost values to calculate its heuristic
                // without calling the possibly expensive heuristic function
                nodeHeuristic = nodeRecord.getEstimatedTotalCost() - nodeRecord.costSoFar;
            } else if (nodeRecord.category == OPEN) { // The node is open

                // If our route is no better, then skip
                if (nodeRecord.costSoFar <= nodeCost) continue;

                // Remove it from the open list (it will be re-added with the new cost)
                openList.remove(nodeRecord);

                // We can use the node's old cost values to calculate its heuristic
                // without calling the possibly expensive heuristic function
                nodeHeuristic = nodeRecord.getEstimatedTotalCost() - nodeRecord.costSoFar;
            } else { // the node is unvisited

                // We'll need to calculate the heuristic value using the function,
                // since we don't have a node record with a previously calculated value
                nodeHeuristic = heuristic.estimate(node, endNode);
            }

            // Update node record's cost and connection
            nodeRecord.costSoFar = nodeCost;
            nodeRecord.from = current.node; //TODO ???

            // Add it to the open list with the estimated total cost
            addToOpenList(nodeRecord, nodeCost + nodeHeuristic);
        }

    }

    protected void generateNodePath(N startNode, GraphPath<N> outPath) {

        // Work back along the path, accumulating nodes
        // outPath.clear();
        while (current.from != null) {
            outPath.add(current.node);
            current = records.get(graph.getIndex(current.from));
        }
        outPath.add(startNode);

        // Reverse the path
        outPath.reverse();
    }

    protected void addToOpenList(NodeRecord<N> nodeRecord, float estimatedTotalCost) {
        openList.add(nodeRecord, estimatedTotalCost);
        nodeRecord.category = OPEN;
    }

    protected NodeRecord<N> getNodeRecord(N node) {
        if(!records.containsKey(graph.getIndex(node))){
            NodeRecord<N> record = new NodeRecord<>();
            record.node = node;
            record.searchId = searchId;
            records.put(graph.getIndex(node), record);
            return record;
        }else{
            return records.get(graph.getIndex(node));
        }
    }

    /**
     * This nested class is used to keep track of the information we need for each node during the search.
     *
     * @param <N> Type of node
     * @author davebaol
     */
    static class NodeRecord<N> extends BinaryHeap.Node {
        /**
         * The reference to the node.
         */
        N node;
        N from;

        /**
         * The incoming connection to the node
         */
        //Connection<N> connection;

        /**
         * The actual cost from the start node.
         */
        float costSoFar;

        /**
         * The node category: {@link #UNVISITED}, {@link #OPEN} or {@link #CLOSED}.
         */
        byte category;

        /**
         * ID of the current search.
         */
        int searchId;

        /**
         * Creates a {@code NodeRecord}.
         */
        public NodeRecord() {
            super(0);
        }

        /**
         * Returns the estimated total cost.
         */
        public float getEstimatedTotalCost() {
            return getValue();
        }
    }
}
