package arc.scene.event;

import arc.Core;
import arc.Graphics.Cursor.SystemCursor;
import arc.scene.Element;

public class IbeamCursorListener extends ClickListener{
    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
        super.enter(event, x, y, pointer, fromActor);
        if(pointer == -1 && event.targetActor.visible){
            Core.graphics.cursor(SystemCursor.ibeam);
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
        super.exit(event, x, y, pointer, toActor);
        if(pointer == -1){
            Core.graphics.restoreCursor();
        }
    }
}
