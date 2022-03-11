package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class LoadingFragment{
    private Table table;
    private TextButton button;
    private Bar bar;
    private Label nameLabel;
    private float progValue;

    public void build(Group parent){
        parent.fill(t -> {
            //rect must fill screen completely.
            t.rect((x, y, w, h) -> {
                Draw.alpha(t.color.a);
                Styles.black8.draw(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
            });
            t.visible = false;
            t.touchable = Touchable.enabled;
            t.add().height(133f).row();
            t.add(new WarningBar()).growX().height(24f);
            t.row();
            nameLabel = t.add("@loading").pad(10f).style(Styles.techLabel).get();
            t.row();
            t.add(new WarningBar()).growX().height(24f);
            t.row();

            text("@loading");

            bar = t.add(new Bar()).pad(3).padTop(6).size(500f, 40f).visible(false).get();
            t.row();
            button = t.button("@cancel", () -> {}).pad(20).size(250f, 70f).visible(false).get();
            table = t;
        });
    }

    public void toFront(){
        table.toFront();
    }

    public void setProgress(Floatp progress){
        bar.reset(0f);
        bar.visible = true;
        bar.set(() -> ((int)(progress.get() * 100) + "%"), progress, Pal.accent);
    }

    public void snapProgress(){
        bar.snap();
    }

    public void setProgress(float progress){
        progValue = progress;
        if(!bar.visible){
            setProgress(() -> progValue);
        }
    }

    public void setButton(Runnable listener){
        button.visible = true;
        button.getListeners().remove(button.getListeners().size - 1);
        button.clicked(listener);
    }

    public void setText(String text){
        text(text);
        nameLabel.setColor(Pal.accent);
    }

    public void show(){
        show("@loading");
    }

    public void show(String text){
        button.visible = false;
        nameLabel.setColor(Color.white);
        bar.visible = false;
        table.clearActions();
        table.touchable = Touchable.enabled;
        text(text);
        table.visible = true;
        table.color.a = 1f;
        table.toFront();
    }

    public void hide(){
        table.clearActions();
        table.toFront();
        table.touchable = Touchable.disabled;
        table.actions(Actions.fadeOut(0.5f), Actions.visible(false));
    }

    private void text(String text){
        nameLabel.setText(text);

        CharSequence realText = nameLabel.getText();

        //fallback to the default font if characters are missing
        //TODO this should happen everywhere
        for(int i = 0; i < realText.length(); i++){
            if(Fonts.tech.getData().getGlyph(realText.charAt(i)) == null){
                nameLabel.setStyle(Styles.defaultLabel);
                return;
            }
        }
        nameLabel.setStyle(Styles.techLabel);
    }
}
