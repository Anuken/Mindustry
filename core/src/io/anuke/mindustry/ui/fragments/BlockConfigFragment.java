package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.layout.Table;

public class BlockConfigFragment implements  Fragment {
    private Table table;
    private Tile configTile;

    @Override
    public void build() {
        table = new Table();
        Core.scene.add(table);
    }

    public boolean isShown(){
        return table.isVisible() && configTile != null;
    }

    public Tile getSelectedTile(){
        return configTile;
    }

    public void showConfig(Tile tile){
        configTile = tile;

        table.clear();
        tile.block().buildTable(tile, table);
        table.pack();
        table.setTransform(true);
        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));

        table.update(()->{
            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy());
            table.setPosition(pos.x, pos.y, Align.center);
            if(configTile == null || configTile.block() == Blocks.air){
                hideConfig();
            }
        });
    }

    public boolean hasConfigMouse(){
        Element e = Core.scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideConfig(){
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
