package mindustry.ui.dialogs;

import arc.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

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
        buttons.addImageTextButton("$back", Icon.left, this::hide).size(210f, 64f);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                Core.app.post(this::hide);
            }
        });
    }
}
