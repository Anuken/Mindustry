package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

public class IconConverter{
    StringBuilder out = new StringBuilder();
    float width, height;

    public static void main(String[] __){

        Log.info("Converting icons...");
        Time.mark();
        Fi.get("fontgen/icons").deleteDirectory();
        Fi.get("fontgen/icon_parts").deleteDirectory();
        Fi[] list = new Fi("icons").list();

        Seq<Fi> files = new Seq<>();

        for(Fi img : list){
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
