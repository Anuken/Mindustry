package arc.packer;

import arc.packer.TexturePacker.*;
import arc.struct.*;

import java.io.*;

/** @author Nathan Sweet */
public class GridPacker implements Packer{
    private final Settings settings;
    /** Where progress is printed. Null means System.out. */
    PrintStream log;

    public GridPacker(Settings settings){
        this.settings = settings;
    }

    @Override
    public Seq<Page> pack(Seq<Rect> inputRects){
        if(!settings.silent) (log == null ? System.out : log).print("| Packing");

        // Rects are packed with right and top padding, so the max size is increased to match. After packing the padding is
        // subtracted from the page size.
        int paddingX = settings.paddingX, paddingY = settings.paddingY;
        int adjustX = paddingX, adjustY = paddingY;
        if(settings.edgePadding){
            if(settings.duplicatePadding){
                adjustX -= paddingX;
                adjustY -= paddingY;
            }else{
                adjustX -= paddingX * 2;
                adjustY -= paddingY * 2;
            }
        }
        int maxWidth = settings.maxWidth + adjustX, maxHeight = settings.maxHeight + adjustY;

        int n = inputRects.size;
        int cellWidth = 0, cellHeight = 0;
        for(int i = 0; i < n; i++){
            Rect rect = inputRects.get(i);
            cellWidth = Math.max(cellWidth, rect.width);
            cellHeight = Math.max(cellHeight, rect.height);
        }
        cellWidth += paddingX;
        cellHeight += paddingY;

        inputRects.reverse();

        Seq<Page> pages = new Seq<>();
        while(inputRects.size > 0){
            Page page = packPage(inputRects, cellWidth, cellHeight, maxWidth, maxHeight);
            page.width -= paddingX;
            page.height -= paddingY;
            pages.add(page);
        }
        return pages;
    }

    private Page packPage(Seq<Rect> inputRects, int cellWidth, int cellHeight, int maxWidth, int maxHeight){
        Page page = new Page();
        page.outputRects = new Seq<>();

        int n = inputRects.size;
        int x = 0, y = 0;
        for(int i = n - 1; i >= 0; i--){
            if(x + cellWidth > maxWidth){
                y += cellHeight;
                if(y > maxHeight - cellHeight) break;
                x = 0;
            }
            Rect rect = inputRects.remove(i);
            rect.x = x;
            rect.y = y;
            rect.width += settings.paddingX;
            rect.height += settings.paddingY;
            page.outputRects.add(rect);
            x += cellWidth;
            page.width = Math.max(page.width, x);
            page.height = Math.max(page.height, y + cellHeight);
        }

        // Flip so rows start at top.
        for(int i = page.outputRects.size - 1; i >= 0; i--){
            Rect rect = page.outputRects.get(i);
            rect.y = page.height - rect.y - rect.height;
        }
        return page;
    }
}
