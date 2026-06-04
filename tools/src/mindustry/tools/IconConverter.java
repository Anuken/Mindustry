package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

public class IconConverter{
    StringBuilder out = new StringBuilder();
    float width, height;

    public static void main(String[] __){
        /*
        Process for adding an icon to the font:

        //CONVERTING A PNG INTO A SVG
        1. Add your png to core\assets-raw\icons\
        2. Run gradle tools:pack. This will add the png to icons.properties
        3. If you haven't done it already, download inkscape and add in path enviroment variables your inkscape installation so "inkscape --etc" commands can run
        4. Run gradle tools:icongen. This step will not work if you havent done step 3
        5. Locate your icon.svg under core\assets-raw\fontgen\extra\
        6. Often generated icons cannot be read by fontello correctly ("If image looks not as expected please convert to compound path manually..."), follow the steps in the wiki linked (https://github.com/fontello/fontello/wiki/How-to-use-custom-images#importing-svg-images)

        //ADDING TO FONTS
        1. Go to Fontello and load the config.json from core/assets-raw/fontgen/config.json
        2. Drag the SVG in
        3. Click on your new fontello icon to select it. If selected the icon circle should show a red outline
        4. Export the config and font file ("Download webfont" button), replace the old config
        5. Intall FontForge if you havent done it already
        6. Take the font (.ttf) from the zip, open it in FontForge, and merge it into font.woff (Element -> Merge fonts) inside core\assets\fonts\. You will be prompted whether you want to keep the existing kerning, usually select Yes
        7. Optionally, go view -> go to (the 0x unicode index, or search by name) to check if the icon has been added
        8. Go to file -> generate fonts, uncheck font validation, click generate and replace the old font.woff. Saving the sfd file is not necessary
        9. Repeat steps 6 and 8 for icon.ttf
        10. Done! do note when using icons if they contain any dashes (-) they will be converted into camelcase. So something like foo-bar.svg becomes Icon.fooBar
        **/

        Log.info("Converting icons...");
        Time.mark();
        Fi.get("fontgen/icons").deleteDirectory();
        Fi.get("fontgen/icon_parts").deleteDirectory();

        Seq<Fi> files = new Seq<>();

        for(Fi img :  new Fi("icons").list()){
            if(img.extension().equals("png")){
                Fi dst = new Fi("fontgen/icons").child(img.nameWithoutExtension().replace("icon-", "") + ".svg");
                new IconConverter().convert(new Pixmap(img), dst);
                dst.copyTo(new Fi("fontgen/icon_parts/").child(dst.name()));
                files.add(dst);
            }
        }

        Seq<String> args = Seq.with("inkscape", "--batch-process", "--actions", "select-all; path-union; fit-canvas-to-selection; export-overwrite; export-do");
        args.addAll(files.map(Fi::absolutePath));

        Fi.get("fontgen/extra").findAll().each(f -> f.copyTo(Fi.get("fontgen/icons").child(f.name())));

        Log.info("Merging paths...");
        Log.info(OS.exec(args.toArray(String.class)));

        Log.info("Done converting icons in &lm@&lgs.", Time.elapsed()/1000f);
        System.exit(0);
    }

    void convert(Pixmap pixmap, Fi output){
        boolean[][] grid = new boolean[pixmap.width][pixmap.height];

        for(int x = 0; x < pixmap.width; x++){
            for(int y = 0; y < pixmap.height; y++){
                grid[x][pixmap.height - 1 - y] = !pixmap.empty(x, y);
            }
        }

        float xscl = 1f, yscl = 1f;//resolution / (float)pixmap.getWidth(), yscl = resolution / (float)pixmap.getHeight();
        float scl = xscl;

        width = pixmap.width;
        height = pixmap.height;

        out.append("<svg width=\"").append(pixmap.width).append("\" height=\"").append(pixmap.height).append("\">\n");

        for(int x = -1; x < pixmap.width; x++){
            for(int y = -1; y < pixmap.height; y++){
                int index = index(x, y, pixmap.width, pixmap.height, grid);

                float leftx = x * xscl, boty = y * yscl, rightx = x * xscl + xscl, topy = y * xscl + yscl,
                midx = x * xscl + xscl / 2f, midy = y * yscl + yscl / 2f;

                switch(index){
                    case 0:
                        break;
                    case 1:
                        tri(
                        leftx, midy,
                        leftx, topy,
                        midx, topy
                        );
                        break;
                    case 2:
                        tri(
                        midx, topy,
                        rightx, topy,
                        rightx, midy
                        );
                        break;
                    case 3:
                        rect(leftx, midy, scl, scl / 2f);
                        break;
                    case 4:
                        tri(
                        midx, boty,
                        rightx, boty,
                        rightx, midy
                        );
                        break;
                    case 5:
                        //ambiguous

                        //7
                        tri(
                        leftx, midy,
                        midx, midy,
                        midx, boty
                        );

                        //13
                        tri(
                        midx, topy,
                        midx, midy,
                        rightx, midy
                        );

                        rect(leftx, midy, scl / 2f, scl / 2f);
                        rect(midx, boty, scl / 2f, scl / 2f);

                        break;
                    case 6:
                        rect(midx, boty, scl / 2f, scl);
                        break;
                    case 7:
                        //invert triangle
                        tri(
                        leftx, midy,
                        midx, midy,
                        midx, boty
                        );

                        //3
                        rect(leftx, midy, scl, scl / 2f);

                        rect(midx, boty, scl / 2f, scl / 2f);
                        break;
                    case 8:
                        tri(
                        leftx, boty,
                        leftx, midy,
                        midx, boty
                        );
                        break;
                    case 9:
                        rect(leftx, boty, scl / 2f, scl);
                        break;
                    case 10:
                        //ambiguous

                        //11
                        tri(
                        midx, boty,
                        midx, midy,
                        rightx, midy
                        );

                        //14
                        tri(
                        leftx, midy,
                        midx, midy,
                        midx, topy
                        );

                        rect(midx, midy, scl / 2f, scl / 2f);
                        rect(leftx, boty, scl / 2f, scl / 2f);

                        break;
                    case 11:
                        //invert triangle

                        tri(
                        midx, boty,
                        midx, midy,
                        rightx, midy
                        );

                        //3
                        rect(leftx, midy, scl, scl / 2f);

                        rect(leftx, boty, scl / 2f, scl / 2f);
                        break;
                    case 12:
                        rect(leftx, boty, scl, scl / 2f);
                        break;
                    case 13:
                        //invert triangle

                        tri(
                        midx, topy,
                        midx, midy,
                        rightx, midy
                        );

                        //12
                        rect(leftx, boty, scl, scl / 2f);

                        rect(leftx, midy, scl / 2f, scl / 2f);
                        break;
                    case 14:
                        //invert triangle

                        tri(
                        leftx, midy,
                        midx, midy,
                        midx, topy
                        );

                        //12
                        rect(leftx, boty, scl, scl / 2f);

                        rect(midx, midy, scl / 2f, scl / 2f);
                        break;
                    case 15:
                        square(midx, midy, scl);
                        break;
                }
            }
        }

        out.append("</svg>");

        output.writeString(out.toString());
    }

    void square(float x, float y, float size){
        rect(x - size/2f, y - size/2f, size, size);
    }

    void tri(float x1, float y1, float x2, float y2, float x3, float y3){
        out.append("<polygon points=\"");
        out.append(x1 + 0.5f).append(",").append(flip(y1 + 0.5f)).append(" ");
        out.append(x2 + 0.5f).append(",").append(flip(y2 + 0.5f)).append(" ");
        out.append(x3 + 0.5f).append(",").append(flip(y3 + 0.5f)).append("\" ");
        out.append("style=\"fill:white\" />\n");
    }

    void rect(float x1, float y1, float width, float height){
        out.append("<rect x=\"")
            .append(x1 + 0.5f).append("\" y=\"").append(flip(y1 + 0.5f) - height)
            .append("\" width=\"").append(width).append("\" height=\"")
            .append(height).append("\" style=\"fill:white\" />\n");
    }

    float flip(float y){
        return height - y;
    }

    int index(int x, int y, int w, int h, boolean[][] grid){
        int botleft = sample(grid, x, y);
        int botright = sample(grid, x + 1, y);
        int topright = sample(grid, x + 1, y + 1);
        int topleft = sample(grid, x, y + 1);
        return (botleft << 3) | (botright << 2) | (topright << 1) | topleft;
    }

    int sample(boolean[][] grid, int x, int y){
        return (x < 0 || y < 0 || x >= grid.length || y >= grid[0].length) ? 0 : grid[x][y] ? 1 : 0;
    }
}
