package arc.graphics;

import arc.*;
import arc.util.*;

/**
 * <p>
 * Represents a mouse cursor. Create a cursor via
 * {@link Graphics#newCursor(Pixmap, int, int)}. To
 * set the cursor use {@link Graphics#setCursor(Cursor)}.
 * To use one of the system cursors, call Graphics#setSystemCursor
 * </p>
 **/
public interface Cursor extends Disposable{

    enum SystemCursor implements Cursor{
        arrow,
        ibeam,
        crosshair,
        hand,
        horizontalResize,
        verticalResize;

        /** The override cursor to use when setting this cursor. */
        public @Nullable Cursor cursor;

        /** Sets the alias for this cursor. */
        public void set(Cursor cursor){
            this.cursor = cursor;
        }

        @Override
        public void dispose(){
            if(cursor != null && !(cursor instanceof SystemCursor)){
                cursor.dispose();
                cursor = null;
            }
        }
    }
}
