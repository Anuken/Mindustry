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
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class BlockDetailsFragment extends Fragment{
    private Table table = new Table(), info = new Table(), power = new Table(), config = new Table(), logic = new Table();
    private Shown shown = Shown.none;
    private Array<Integer> offset = Array.with(0, 0, 0, 0);
    private Array<Runnable> runnable = new Array<>();
    private InputHandler input;
    private Tile detailsTile;
    private Block detailsBlock;

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
        return table.isVisible() && detailsTile != null;
    }

    public boolean shouldUpdate(){
        return shown == Shown.info || shown == Shown.power;
    }

    public Tile getSelectedTile(){
        return detailsTile;
    }

    public void showDetails(Tile tile){
        showDetails(tile, false);
    }

    public void showDetails(Tile tile, boolean update){
        detailsTile = tile;
        detailsBlock = tile.block();

        info.clear();
        power.clear();
        config.clear();
        logic.clear();
        table.clear();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        if(detailsBlock.buildInfo(tile, info)){
            offset.set(0, group.getButtons().size);
            showTable(tile, info, Shown.info, offset.get(0), group, "icon-info");
            updateTable(info, offset.get(0), update);
        }
        if(detailsBlock.buildPower(tile, power)){
            offset.set(1, group.getButtons().size);
            showTable(tile, power, Shown.power, offset.get(1), group, "icon-power");
            updateTable(power, offset.get(1), update);
        }
        if(detailsBlock.buildConfig(tile, config)){
            offset.set(2, group.getButtons().size);
            showTable(tile, config, Shown.config, offset.get(2), group, "icon-config");
        }
        if (detailsBlock.buildLogic(tile, logic)){
            offset.set(3, group.getButtons().size);
            showTable(tile, logic, Shown.logic, offset.get(3), group, "icon-logic");
        }
        detailsBlock.buildTable(tile, table);

        table.row();
        for(int i = 0; i < runnable.size; i++) runnable.get(i).run();
        runnable.clear();

        table.pack();
        table.setVisible(true);
        table.setTransform(true);
        if(shouldUpdate() && update)
            table.setScale(1f, 1f);
        else{
            table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                    Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));
        }

        table.update(() -> {
            if(state.is(State.menu)){
                hideDetails();
                return;
            }

            if(detailsTile != null && detailsTile.block().shouldHideDetails(detailsTile, input.player)){
                hideDetails();
                return;
            }

            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y, Align.top);
            if(detailsTile == null || detailsTile.block() == Blocks.air || detailsTile.block() != detailsBlock){
                hideDetails();
            }
        });
    }

    private void showTable(Tile tile, Table table, Shown shown, int offset, ButtonGroup<ImageButton> group, String icon){
        this.table.addImageButton(icon, "clear", 16 * 2f, () -> {
            runnable.add(() -> {
                if(this.shown != shown){
                    for(int i = 0; i < offset; i++) this.table.add();
                    this.table.add(table);
                    this.shown = shown;
                }else this.shown = Shown.none;
            });
            showDetails(tile);
        }).group(group);
    }

    private void updateTable(Table table, int offset, boolean update){
        if(shouldUpdate() && update){
            runnable.add(() -> {
                for (int i = 0; i < offset; i++) this.table.add();
                this.table.add(table);
            });
        }
    }

    public boolean hasDetailsMouse(){
        Element e = Core.scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideDetails(){
        detailsTile = null;
        detailsBlock = null;
        offset.clear();
        offset.setSize(4);
        shown = Shown.none;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
