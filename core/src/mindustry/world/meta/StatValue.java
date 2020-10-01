package mindustry.world.meta;

import arc.scene.ui.layout.Table;

/**
 * A base interface for a value of a stat that is displayed.
 */
public interface StatValue{
    /**
     * This method should provide all elements necessary to display this stat to the specified table.
     * For example, a stat that is just text would add a label to the table.
     */
    void display(Table table);
}
