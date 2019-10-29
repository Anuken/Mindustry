package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.input.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;

import static io.anuke.mindustry.Vars.*;

public class FloatingDialog extends Dialog{
    private boolean wasPaused;
    protected boolean shouldPause;

    public FloatingDialog(String title, DialogStyle style){
        super(title, style);
        setFillParent(true);
        this.title.setAlignment(Align.center);
        titleTable.row();
        titleTable.addImage(Tex.whiteui, Pal.accent)
        .growX().height(3f).pad(4f);

        hidden(() -> {
            if(shouldPause && !state.is(State.menu)){
                if(!wasPaused || net.active()){
                    state.set(State.playing);
                }
            }
            Sounds.back.play();
        });

        shown(() -> {
            if(shouldPause && !state.is(State.menu)){
                wasPaused = state.is(State.paused);
                state.set(State.paused);
            }
        });
    }

    public FloatingDialog(String title){
        this(title, Core.scene.getStyle(DialogStyle.class));
    }

    protected void onResize(Runnable run){
        Events.on(ResizeEvent.class, event -> {
            if(isShown() && Core.scene.getDialog() == this){
                run.run();
                updateScrollFocus();
            }
        });
    }

    @Override
    public void addCloseButton(){
        buttons.defaults().size(210f, 64f);
        buttons.addImageTextButton("$back", Icon.arrowLeft, this::hide).size(210f, 64f);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                Core.app.post(this::hide);
            }
        });
    }
}
