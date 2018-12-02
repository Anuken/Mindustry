package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
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

public class BlockDetailsFragment extends Fragment{
    private Table table = new Table(), info = new Table(), power = new Table(), config = new Table(), logic = new Table();
    private Shown shown = Shown.none;
    private Array<Integer> offset = new Array<>(new Integer[]{0, 0, 0, 0});
    private Array<Runnable> runnable = new Array<>();
    private InputHandler input;
    private Tile configTile;
    private Block configBlock;

    private enum Shown{
        none,
        info,
        power,
        config,
        logic
    }

    public BlockDetailsFragment(InputHandler input){
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

    public void showDetails(Tile tile){
        configTile = tile;
        configBlock = tile.block();

        info.clear();
        power.clear();
        config.clear();
        logic.clear();
        table.clear();

        int offset = 0;
        if (configBlock.buildInfo(tile, info)){
            this.offset.set(0, offset++);
            table.addImageButton("icon-info", "clear-partial", 16 * 2f, () -> {
                if (shown != Shown.info) runnable.add(() -> {
                    // for (int i = 0; i < this.offset.get(0); i++) table.add();
                    table.add(info);
                    shown = Shown.info;
                });
                else shown = Shown.none;
                showDetails(tile);
            });
        }
        if (configBlock.buildPower(tile, power)){
            this.offset.set(1, offset++);
            table.addImageButton("icon-power", "clear-partial", 16 * 2f, () -> {
                if (shown != Shown.power) runnable.add(() -> {
                    for (int i = 0; i < this.offset.get(1); i++) table.add();
                    table.add(power);
                    shown = Shown.power;
                });
                else shown = Shown.none;
                showDetails(tile);
            });
        }
        if (configBlock.buildConfig(tile, config)){
            this.offset.set(2, offset++);
            table.addImageButton("icon-config", "clear-partial", 16 * 2f, () -> {
                if (shown != Shown.config) runnable.add(() -> {
                    for (int i = 0; i < this.offset.get(2); i++) table.add();
                    table.add(config);
                    shown = Shown.config;
                });
                else shown = Shown.none;
                showDetails(tile);
            });
        }
        if (configBlock.buildLogic(tile, logic)){
            this.offset.set(3, offset++);
            table.addImageButton("icon-logic", "clear-partial", 16 * 2f, () -> {
                if (shown != Shown.logic) runnable.add(() -> {
                    for (int i = 0; i < this.offset.get(3); i++) table.add();
                    table.add(logic);
                    shown = Shown.logic;
                });
                else shown = Shown.none;
                showDetails(tile);
            });
        }
        configBlock.buildTable(tile, table);

        if (!runnable.isEmpty()) table.row();
        for (int i = 0; i < runnable.size; i++) runnable.get(i).run();
        runnable.clear();

        table.pack();
        table.setVisible(true);
        table.setTransform(true);
        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));

        table.update(() -> {
            if(state.is(State.menu)){
                hideDetails();
                return;
            }

            if(configTile != null && configTile.block().shouldHideDetails(configTile, input.player)){
                hideDetails();
                return;
            }

            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y, Align.top);
            if(configTile == null || configTile.block() == Blocks.air || configTile.block() != configBlock){
                hideDetails();
            }
        });
    }

    public boolean hasDetailsMouse(){
        Element e = Core.scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideDetails(){
        configTile = null;
        offset.clear();
        offset.setSize(4);
        shown = Shown.none;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
