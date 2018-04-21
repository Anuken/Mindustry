package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.HierarchicalGraph;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.Tile;

public class HGraph implements HierarchicalGraph<Tile> {

    @Override
    public int getLevelCount() {
        return 0;
    }

    @Override
    public void setLevel(int level) {

    }

    @Override
    public Tile convertNodeBetweenLevels(int inputLevel, Tile node, int outputLevel) {
        return null;
    }

    @Override
    public Array<Connection<Tile>> getConnections(Tile fromNode) {
        return null;
    }
}
