package io.anuke.mindustry.ai;

/**An interface for an indexed graph that doesn't use allocations for connections.*/
public interface OptimizedGraph<N>{
    /**This is used in the same way as getConnections(), but does not use Connection objects.*/
    N[] connectionsOf(N node);

    /** Returns the unique index of the given node.
     * @param node the node whose index will be returned
     * @return the unique index of the given node. */
    int getIndex (N node);
}
