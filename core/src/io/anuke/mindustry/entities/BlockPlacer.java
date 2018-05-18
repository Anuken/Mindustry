package io.anuke.mindustry.entities;

import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.resource.Recipe;

public interface BlockPlacer {
    void addPlaceBlock(PlaceRequest place);
    Team getTeam();

    class PlaceRequest{
        public final int x, y, rotation;
        public final Recipe recipe;

        public PlaceRequest(int x, int y, int rotation, Recipe recipe) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.recipe = recipe;
        }
    }
}
