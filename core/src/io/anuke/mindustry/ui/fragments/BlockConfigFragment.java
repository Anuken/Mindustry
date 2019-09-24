package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class BlockConfigFragment extends Fragment{
    private Table table = new Table();
    private Tile configTile;
    private Block configBlock;

    @Override
    public void build(Group parent){
        table.visible(false);
        parent.addChild(table);

        //hacky way to hide block config when in menu
        //TODO remove?
        Core.scene.add(new Element(){
            @Override
            public void act(float delta){
                super.act(delta);
                if(state.is(State.menu)){
                    table.visible(false);
                    configTile = null;
                }
            }
        });
    }

    public boolean isShown(){
        return table.isVisible() && configTile != null;
    }

    public Tile getSelectedTile(){
        return configTile;
    }

    public void showConfig(Tile tile){
        configTile = tile;
        configBlock = tile.block();

        table.visible(true);
        table.clear();
        tile.block().buildTable(tile, table);
        table.pack();
        table.setTransform(true);
        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
        Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));

        table.update(() -> {
            if(configTile != null && configTile.block().shouldHideConfigure(configTile, player)){
                hideConfig();
                return;
            }

            table.setOrigin(Align.center);
            if(configTile == null || configTile.block() == Blocks.air || configTile.block() != configBlock){
                hideConfig();
            }else{
                configTile.block().updateTableAlign(tile, table);
            }
        });
    }

    public boolean hasConfigMouse(){
        Element e = Core.scene.hit(Core.input.mouseX(), Core.graphics.getHeight() - Core.input.mouseY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideConfig(){
        configTile = null;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
