package arc.scene.ui.layout;

import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;

/** A table that autowraps instead of using standard table layout. */
public class WrapTable extends Table{
    private static FloatSeq rowHeights = new FloatSeq();
    private static IntSeq cellRow = new IntSeq();
    private float lastComputeWidth = -1;

    public WrapTable(){
    }

    public WrapTable(Drawable background){
        super(background);
    }

    public WrapTable(Drawable background, Cons<Table> cons){
        super(background, cons);
    }

    public WrapTable(Cons<Table> cons){
        super(cons);
    }

    @Override
    protected void computeSize(){
        sizeInvalid = false;

        Seq<Cell> cells = this.cells;
        int cellCount = cells.size;

        float maxCellWidth = 0, totalWidth = 0;

        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);

            c.computedPadLeft = c.padLeft;
            c.computedPadTop = c.padTop;
            c.computedPadRight = c.padRight;
            c.computedPadBottom = c.padBottom;

            float prefWidth = c.prefWidth();
            float prefHeight = c.prefHeight();
            float minWidth = c.minWidth();
            float minHeight = c.minHeight();
            float maxWidth = c.maxWidth();
            float maxHeight = c.maxHeight();
            if(prefWidth < minWidth) prefWidth = minWidth;
            if(prefHeight < minHeight) prefHeight = minHeight;
            if(maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth;
            if(maxHeight > 0 && prefHeight > maxHeight) prefHeight = maxHeight;

            c.elementWidth = prefWidth;
            c.elementHeight = prefHeight;

            float cellWidth = prefWidth + c.computedPadLeft + c.computedPadRight;

            maxCellWidth = Math.max(maxCellWidth, cellWidth);
            totalWidth += cellWidth;
        }

        float hpadding = getMarginLeft() + getMarginRight();
        float vpadding = getMarginTop() + getMarginBottom();

        tableMinWidth = maxCellWidth + hpadding;
        tablePrefWidth = totalWidth + hpadding;

        float maxRowWidth = lastComputeWidth >= 0 ? Math.max(maxCellWidth, lastComputeWidth) : maxCellWidth;

        float contentHeight = wrapHeight(maxRowWidth);
        tableMinHeight = contentHeight + vpadding;
        tablePrefHeight = tableMinHeight;
    }

    float wrapHeight(float maxRowWidth){
        Seq<Cell> cells = this.cells;
        int cellCount = cells.size;

        float currentX = 0, currentY = 0, rowHeight = 0;
        for(int i = 0; i < cellCount; i++){
            Cell c = cells.get(i);
            float cellWidth = c.elementWidth + c.computedPadLeft + c.computedPadRight;
            float cellHeight = c.elementHeight + c.computedPadTop + c.computedPadBottom;

            if(currentX > 0 && currentX + cellWidth > maxRowWidth){
                currentY += rowHeight;
                currentX = 0;
                rowHeight = 0;
            }

            currentX += cellWidth;
            rowHeight = Math.max(rowHeight, cellHeight);
        }
        return currentY + rowHeight;
    }

     @Override
    protected void layout(float layoutX, float layoutY, float layoutWidth, float layoutHeight){
         Seq<Cell> cells = this.cells;
         int cellCount = cells.size;

         if(sizeInvalid) computeSize();

         float padLeft = getMarginLeft();
         float hpadding = padLeft + getMarginRight();
         float padTop = getMarginTop();
         float vpadding = padTop + getMarginBottom();

         float maxRowWidth = Math.max(0f, layoutWidth - hpadding);

         // Recheck height against the real width, since computeSize may have guessed before width was set.
         if(Math.abs(maxRowWidth - lastComputeWidth) > 0.001f){
             lastComputeWidth = maxRowWidth;
             float newMinHeight = wrapHeight(maxRowWidth) + vpadding;
             if(Math.abs(newMinHeight - tableMinHeight) > 0.001f){
                 tableMinHeight = newMinHeight;
                 tablePrefHeight = newMinHeight;
                 invalidateHierarchy();
             }
         }

         rowHeights.clear();
         cellRow.clear();

         float currentX = 0, rowHeight = 0, contentWidth = 0;
         int row = 0;
         for(int i = 0; i < cellCount; i++){
             Cell c = cells.get(i);
             float cellWidth = c.elementWidth + c.computedPadLeft + c.computedPadRight;
             float cellHeight = c.elementHeight + c.computedPadTop + c.computedPadBottom;

             if(currentX > 0 && currentX + cellWidth > maxRowWidth){
                 rowHeights.add(rowHeight);
                 contentWidth = Math.max(contentWidth, currentX);
                 currentX = 0;
                 rowHeight = 0;
                 row++;
             }

             cellRow.add(row);
             currentX += cellWidth;
             rowHeight = Math.max(rowHeight, cellHeight);
         }
         rowHeights.add(rowHeight);
         contentWidth = Math.max(contentWidth, currentX);

         float contentHeight = 0;
         for(int i = 0; i < rowHeights.size; i++)
             contentHeight += rowHeights.items[i];

         float tableWidth = contentWidth + hpadding;
         float tableHeight = contentHeight + vpadding;

         int align = this.align;
         float x = layoutX + padLeft;
         if((align & Align.right) != 0)
             x += layoutWidth - tableWidth;
         else if((align & Align.left) == 0) // Center
             x += (layoutWidth - tableWidth) / 2;

         float y = layoutY + padTop;
         if((align & Align.bottom) != 0)
             y += layoutHeight - tableHeight;
         else if((align & Align.top) == 0) // Center
             y += (layoutHeight - tableHeight) / 2;

         currentX = 0;
         float currentY = y;
         row = 0;
         for(int i = 0; i < cellCount; i++){
             Cell c = cells.get(i);
             float cellWidth = c.elementWidth + c.computedPadLeft + c.computedPadRight;

             if(cellRow.items[i] != row){
                 currentY += rowHeights.items[row];
                 currentX = 0;
                 row = cellRow.items[i];
             }

             c.elementX = x + currentX + c.computedPadLeft;
             c.elementY = currentY + (rowHeights.items[row] - c.elementHeight + c.computedPadTop - c.computedPadBottom) / 2;

             currentX += cellWidth;
         }
    }
}
