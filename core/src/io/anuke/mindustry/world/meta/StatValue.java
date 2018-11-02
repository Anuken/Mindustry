package io.anuke.mindustry.world.meta;

import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.Tooltip;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;

/**
 * A base interface for a value of a stat that is displayed.
 */
public interface StatValue{
    /**
     * This method should provide all elements necessary to display this stat to the specified table.
     * For example, a stat that is just text would add label to the table.
     */
    void display(Table table);

    /**
     * This method adds an icon image together with a tool tip which contains the name of the item.
     * @param table the table to add the image cell to.
     * @param item The item which provides the tool tip content.
     * @return the image cell which was created. The cell is not yet sized or padded.
     */
    static Cell<Image> addImageWithToolTip(Table table, UnlockableContent item){

        // Create a table cell with a new image as provided by the item
        Cell<Image> imageCell = table.addImage(item.getContentIcon());

        // Retrieve the image and add a tool tip with the item's name
        addToolTip(imageCell.getElement(), item);

        // Return the table cell for further processing (sizing, padding, ...)
        return imageCell;
    }

    /**
     * Adds a tool tip containing the item's localized name to the given element.
     * @param element The element to assign the tool tip to.
     * @param item The item which provides the tool tip content.
     */
    static void addToolTip(Element element, UnlockableContent item){
        element.addListener(new Tooltip<>(new Table("clear"){{
            add(item.localizedName());
            margin(4);
        }}));
    }
}
