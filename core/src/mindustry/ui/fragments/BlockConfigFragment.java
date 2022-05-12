package mindustry.ui.fragments;

import arc.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class BlockConfigFragment{
    Table table = new Table();
    Building selected;

    public void build(Group parent){
        table.visible = false;
        parent.addChild(table);

        Events.on(ResetEvent.class, e -> forceHide());
    }

    public void forceHide(){
        table.visible = false;
        selected = null;
    }

    public boolean isShown(){
        return table.visible && selected != null;
    }

    public Building getSelected(){
        return selected;
    }

    public void showConfig(Building tile){
        if(selected != null) selected.onConfigureClosed();
        if(tile.configTapped()){
            selected = tile;

            table.visible = true;
            table.clear();
            tile.buildConfiguration(table);
            table.pack();
            table.setTransform(true);
            table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
            Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out));

            table.update(() -> {
                if(selected != null && selected.shouldHideConfigure(player)){
                    hideConfig();
                    return;
                }

                table.setOrigin(Align.center);
                if(selected == null || selected.block == Blocks.air || !selected.isValid()){
                    hideConfig();
                }else{
                    selected.updateTableAlign(table);
                }
            });
        }
    }

    public boolean hasConfigMouse(){
        Element e = Core.scene.hit(Core.input.mouseX(), Core.graphics.getHeight() - Core.input.mouseY(), true);
        return e != null && (e == table || e.isDescendantOf(table));
    }

    public void hideConfig(){
        if(selected != null) selected.onConfigureClosed();
        selected = null;
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interp.pow3Out), Actions.visible(false));
    }
}
