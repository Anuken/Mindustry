package mindustry.ui;

import arc.scene.ui.layout.*;

/** An interface for things that can be displayed when hovered over. */
public interface Displayable{
    default boolean displayable(){
        return true;
    }

    void display(Table table);
}
