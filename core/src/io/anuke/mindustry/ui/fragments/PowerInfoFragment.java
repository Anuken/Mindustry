package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class PowerInfoFragment extends Fragment{
    private Table table;
    private boolean visible;

    @Override
    public void build(Group parent){
        table = new Table();
        table.left().bottom();
        table.visible(() -> !state.is(State.menu) && visible);
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void show(Tile tile){
        if(tile == null || tile.block() == null || !tile.block().hasPower) return;
        if (tile.entity == null || tile.entity.power == null) return;
        PowerGraph graph = tile.entity.power.graph;

        table.clearChildren();
        table.background("inventory");


        table.label(() -> {
            String info = Bundles.format("text.powerinfo.produce", round(graph.lastAmountInfo.avgProduce));
            if (graph.lastAmountInfo.avgProduce - graph.lastAmountInfo.avgConsume < -0.1f) {
                return "[red]" + info + "[]";
            } else {
                return info;
            }
        }).width(200f).left();
        table.row();
        // table.left().defaults().fillX();
        table.label(() -> Bundles.format("text.powerinfo.consume", round(graph.lastAmountInfo.avgConsume))).left();
        table.row();

        table.label(() -> Bundles.format("text.powerinfo.buffer", round(graph.lastAmountInfo.buffer))).left();
        table.row();

        table.update(() -> {

            if(state.is(State.menu)){
                hide();
                return;
            }

            Vector2 v = Graphics.screen(tile.drawx() + tile.block().size * tilesize / 2f, tile.drawy() + tile.block().size * tilesize / 2f);
            table.pack();
            table.setPosition(v.x, v.y, Align.topLeft);
        });

        table.act(0.2f);
        visible = true;
    }

    public void hide(){
        if (!visible) return;
        table.clear();
        table.update(() -> {
        });
        visible = false;
    }

    private String round(float f){
        if(f >= 1000000){
            return Strings.toFixed(f / 1000000f, 2) + "[gray]mil[]";
        }else if(f >= 1000){
            return Strings.toFixed(f / 1000, 2) + "k";
        }else{
            return Strings.toFixed(f, 1);
        }
    }

}
