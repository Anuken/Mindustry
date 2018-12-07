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
    private Group group = new Group();
    private Table table = new Table(), info = new Table(), power = new Table(), config = new Table(), logic = new Table();
    private Array<Runnable> runnable = new Array<>();

    private InputHandler input;
    private Tile detailsTile;
    private Block detailsBlock;

    public BlockDetailsFragment(InputHandler input){
        this.input = input;
    }

    @Override
    public void build(Group parent){
        parent.addChild(table);

        group.setFillParent(true);
        parent.addChild(group);

        group.addChild(info);
        group.addChild(power);
        group.addChild(config);
        group.addChild(logic);
    }

    public boolean isShown(){
        return table.isVisible() && detailsTile != null;
    }

    public Tile getSelectedTile(){
        return detailsTile;
    }

    public void showDetails(Tile tile){
        detailsTile = tile;
        detailsBlock = tile.block();

        table.clear();
        info.clear();
        power.clear();
        config.clear();
        logic.clear();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        if(detailsBlock.buildInfo(tile, info, true)){
            showTable(info, "icon-info", group);
            updateTable(tile, info, () -> detailsBlock.buildInfo(tile, info));
        }
        if(detailsBlock.buildPower(tile, power, true)){
            showTable(power, "icon-power", group);
            updateTable(tile, power, () -> detailsBlock.buildPower(tile, power));
        }
        if(detailsBlock.buildConfig(tile, config, true)){
            showTable(config, "icon-config", group);
            updateTable(tile, config, () -> detailsBlock.buildConfig(tile, config));
        }
        if (detailsBlock.buildLogic(tile, logic, true)){
            showTable(logic, "icon-logic", group);
            updateTable(tile, logic, () -> detailsBlock.buildLogic(tile, logic));
        }
        detailsBlock.buildTable(tile, table);

        table.pack();
        table.setVisible(true);
        table.setTransform(true);
        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));

        for(Runnable runnable : runnable) runnable.run();
        runnable.clear();

        table.update(() -> {
            if(state.is(State.menu)){
                hideDetails();
                return;
            }

            if(detailsTile != null && detailsTile.block().shouldHideDetails(detailsTile, input.player)){
                hideDetails();
                return;
            }

            if(detailsTile == null || detailsTile.block() == Blocks.air || detailsTile.block() != detailsBlock){
                hideDetails();
                return;
            }

            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y, Align.top);
        });
    }

    private void showTable(Table table, String icon, ButtonGroup<ImageButton> group){
        ImageButton button = this.table.addImageButton(icon, "clear-toggle", 16 * 2f, () -> {}).group(group).get();
        button.changed(() -> {
            if(button.isChecked()){
                table.setVisible(true);
                table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                        Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));
            }else table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
        });
        runnable.add(() -> {
            table.pack();
            table.setVisible(false);
            table.setTransform(true);
        });
    }

    private void updateTable(Tile tile, Table table, Runnable runnable){
        table.update(() -> {
            if(state.is(State.menu)){
                stopUpdate(table);
                return;
            }

            if(detailsTile != null && detailsTile.block().shouldHideDetails(detailsTile, input.player)){
                stopUpdate(table);
                return;
            }

            if(detailsTile == null || detailsTile.block() == Blocks.air || detailsTile.block() != detailsBlock){
                stopUpdate(table);
                return;
            }

            runnable.run();

            table.setOrigin(Align.center);
            Vector2 pos = Graphics.screen(tile.drawx(), tile.drawy() - tile.block().size * tilesize / 2f - 1);
            table.setPosition(pos.x, pos.y - this.table.getHeight(), Align.top);
        });
    }

    private void stopUpdate(Table table){
        table.forEach(element -> element.update(() -> {}));
    }

    public boolean hasDetailsMouse(){
        Element e = Core.scene.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideDetails(){
        stopUpdate(table);
        detailsTile = null;
        for(Element element : group.getChildren()) element.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false));
    }
}
