package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.Label;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.*;

public class LoadingFragment extends Fragment{
    private Table table;
    private TextButton button;
    private Bar bar;

    @Override
    public void build(Group parent){
        parent.fill(Styles.black8, t -> {
            t.visible(false);
            t.touchable(Touchable.enabled);
            t.add().height(133f).row();

            t.addImage().growX().height(3f).pad(4f).growX().get().setColor(Pal.accent);
            t.row();
            t.add("$loading").name("namelabel").pad(10f);
            t.row();
            t.addImage().growX().height(3f).pad(4f).growX().get().setColor(Pal.accent);
            t.row();

            bar = t.add(new Bar()).pad(3).size(500f, 40f).visible(false).get();
            t.row();
            button = t.addButton("$cancel", () -> {}).pad(20).size(250f, 70f).visible(false).get();
            table = t;
        });
    }

    public void setProgress(FloatProvider progress){
        bar.reset(0f);
        bar.visible(true);
        bar.set(() -> ((int)(progress.get() * 100) + "%"), progress, Pal.accent);
    }

    public void setButton(Runnable listener){
        button.visible(true);
        button.getListeners().remove(button.getListeners().size - 1);
        button.clicked(listener);
    }

    public void setText(String text){
        table.<Label>find("namelabel").setText(text);
        table.<Label>find("namelabel").setColor(Pal.accent);
    }

    public void show(){
        show("$loading");
    }

    public void show(String text){
        table.<Label>find("namelabel").setColor(Color.white);
        bar.visible(false);
        table.clearActions();
        table.touchable(Touchable.enabled);
        table.<Label>find("namelabel").setText(text);
        table.visible(true);
        table.getColor().a = 1f;
        table.toFront();
    }

    public void hide(){
        table.clearActions();
        table.toFront();
        table.touchable(Touchable.disabled);
        table.actions(Actions.fadeOut(0.5f), Actions.visible(false));
    }
}
