package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class BaseDialog extends Dialog{
    protected boolean wasPaused;
    /** If true, this dialog will pause the game while open. */
    protected boolean shouldPause;

    public BaseDialog(String title, DialogStyle style){
        super(title, style);
        setFillParent(true);
        this.title.setAlignment(Align.center);
        titleTable.row();
        titleTable.image(Tex.whiteui, Pal.accent)
        .growX().height(3f).pad(4f);

        hidden(() -> {
            if(shouldPause && state.isGame() && !net.active() && !wasPaused){
                state.set(State.playing);
            }
            Sounds.back.play();
        });

        shown(() -> {
            if(shouldPause && state.isGame() && !net.active()){
                wasPaused = state.is(State.paused);
                state.set(State.paused);
            }
        });
    }

    public BaseDialog(String title){
        this(title, Core.scene.getStyle(DialogStyle.class));
    }

    /** Places the buttons as an overlay on top of the content. Used when the content can be scrolled through.*/
    protected void makeButtonOverlay(){
        clearChildren();
        add(titleTable).growX().row();
        stack(cont, buttons).grow();
        buttons.bottom();
    }

    protected void onResize(Runnable run){
        Events.on(ResizeEvent.class, event -> {
            if(isShown() && Core.scene.getDialog() == this){
                run.run();
                updateScrollFocus();
            }
        });
    }

    public void addCloseListener(){
       closeOnBack();
    }

    public void addCloseButton(float width){
        buttons.defaults().size(width, 64f);
        buttons.button("@back", Icon.left, this::hide).size(width, 64f);

        addCloseListener();
    }

    @Override
    public void addCloseButton(){
        addCloseButton(210f);
    }
}
