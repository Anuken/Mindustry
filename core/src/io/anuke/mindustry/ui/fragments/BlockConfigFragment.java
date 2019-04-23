package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class BlockConfigFragment extends Fragment{
    private Table table = new Table();
    private Tile configTile;
    private Block configBlock;

    @Override
    public void build(Group parent){
        table.visible(false);
        parent.addChild(table);
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
            if(state.is(State.menu)){
                hideConfig();
                return;
            }

            if(configTile != null && configTile.block().shouldHideConfigure(configTile, player)){
                hideConfig();
                return;
            }

            table.setOrigin(Align.center);
            Vector2 pos = Core.input.mouseScreen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y, Align.top);
            if(configTile == null || configTile.block() == Blocks.air || configTile.block() != configBlock){
                hideConfig();
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
