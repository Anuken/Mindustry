package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;

/**An interface for an indexed graph that doesn't use allocations for connections.*/
public interface OptimizedGraph<N> extends IndexedGraph<N> {
    /**This is used in the same way as getConnections(), but does not use Connection objects.*/
    N[] connectionsOf(N node);
}
