package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.scene.ui.layout.Table;

public interface Mission{
    boolean isComplete();
    String displayString();
    GameMode getMode();
    void display(Table table);

    default void generate(Tile[][] tiles, Sector sector){}
}
