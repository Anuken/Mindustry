package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class LoadingFragment{
    private Table table;
    private TextButton button;
    private Bar bar;
    private Label nameLabel;
    private @Nullable Runnable cancelListener;
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
            nameLabel = t.add("@loading").pad(10f).style(Styles.outlineLabel).get();
            t.row();
            t.add(new WarningBar()).growX().height(24f);
            t.row();

            nameLabel.setText("@loading");

            bar = t.add(new Bar()).pad(3).padTop(6).size(500f, 40f).visible(false).get();
            t.row();
            button = t.button("@cancel", () -> {
                if(cancelListener != null){
                    cancelListener.run();
                }
            }).pad(20).size(250f, 70f).visible(false).get();
            button.keyDown(key -> {
                if(cancelListener != null && (key == KeyCode.back || key == KeyCode.escape)){
                    cancelListener.run();
                }
            });
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

    public void showProgressBar(){
        if(!bar.visible){
            setProgress(() -> progValue);
        }
    }

    public boolean showingProgress(){
        return bar.visible;
    }

    public void setButton(Runnable listener){
        setButton(listener, true);
    }

    public void setButton(Runnable listener, boolean showProgress){
        button.visible = showProgress;
        button.requestKeyboard();
        cancelListener = listener;
    }

    public void setText(String text){
        nameLabel.setText(text);
    }

    public void setText(String text, Color color){
        nameLabel.setText(text);
        nameLabel.setColor(color);
    }

    public void show(){
        show("@loading");
    }

    public void show(String text){
        button.visible = false;
        cancelListener = null;
        bar.visible = false;
        table.clearActions();
        table.touchable = Touchable.enabled;
        nameLabel.setColor(Color.white);
        nameLabel.setText(text);
        table.visible = true;
        table.color.a = 1f;
        table.toFront();
    }

    public void hide(){
        table.clearActions();
        table.toFront();
        button.visible = false;
        table.touchable = Touchable.disabled;
        table.actions(Actions.fadeOut(0.5f), Actions.visible(false));

        if(Core.scene.getKeyboardFocus() == button){
            Core.scene.setKeyboardFocus(null);
        }
    }

}
