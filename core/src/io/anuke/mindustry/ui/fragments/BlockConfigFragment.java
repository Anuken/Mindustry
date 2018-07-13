package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class BlockConfigFragment extends Fragment{
    private Table table = new Table();
    private InputHandler input;
    private Tile configTile;
    private Block configBlock;

    public BlockConfigFragment(InputHandler input){
        this.input = input;
    }

    @Override
    public void build(Group parent){
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

        table.setVisible(true);
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

            if(configTile != null && configTile.block().shouldHideConfigure(configTile, input.player)){
                hideConfig();
                return;
            }

            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y, Align.top);
            if(configTile == null || configTile.block() == Blocks.air || configTile.block() != configBlock){
                hideConfig();
            }
        });
    }

    public boolean hasConfigMouse(){
        Element e = Core.scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideConfig(){
        configTile = null;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
