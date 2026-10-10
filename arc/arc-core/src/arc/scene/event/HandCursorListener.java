package arc.scene.event;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.utils.*;

public class HandCursorListener extends ClickListener{
    public Boolp enabled = () -> true;
    public boolean checkEnabled = true;

    public HandCursorListener(Boolp enabled, boolean check){
        this.enabled = enabled;
        this.checkEnabled = check;
    }

    public HandCursorListener(){
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
        super.enter(event, x, y, pointer, fromActor);

        if(pointer != -1 || !enabled.get() || (checkEnabled && (isDisabled(event.targetActor) || isDisabled(fromActor)))){
            return;
        }

        Core.graphics.cursor(SystemCursor.hand);
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
        super.exit(event, x, y, pointer, toActor);

        if(pointer == -1){
            Core.graphics.restoreCursor();
        }
    }

    static boolean isDisabled(Element element){
        return element != null && (((element instanceof Disableable && ((Disableable)element).isDisabled()) || !element.visible) || isDisabled(element.parent));
    }
}
