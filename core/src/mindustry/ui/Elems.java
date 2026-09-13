package mindustry.ui;

import arc.func.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.gen.*;

public class Elems{

    /** Fancy check box with a background. Not a real check box. */
    public static Button check(String text, Boolp checked, Boolc listener){
        return check(text, checked != null && checked.get(), checked, listener);
    }

    /** Fancy check box with a background. Not a real check box. */
    public static Button check(String text, boolean checked, Boolc listener){
        return check(text, checked, null, listener);
    }

    /** Fancy check box with a background. Not a real check box. */
    public static Button check(String text, boolean currentChecked, @Nullable Boolp checked, Boolc listener){
        Button box = new Button(Styles.grayt);
        box.background(Styles.grayPanel);
        box.margin(10f);

        box.add(new Image()).update(i -> i.setDrawable(box.isOver() ? (box.isChecked() ? Tex.checkOnOver : Tex.checkOver) : box.isChecked() ? Tex.checkOn : Tex.checkOff))
        .size(32f).padRight(8f).padLeft(-4f);

        box.add(text);
        box.setChecked(currentChecked);

        if(checked != null) box.update(() -> box.setChecked(checked.get()));

        box.clicked(() -> listener.get(box.isChecked()));

        box.left();

        return box;
    }
}
