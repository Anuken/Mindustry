package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LogicDisplay extends Block{
    public static final byte
        commandClear = 0,
        commandColor = 1,
        commandStroke = 2,
        commandLine = 3,
        commandRect = 4,
        commandLineRect = 5,
        commandPoly = 6,
        commandLinePoly = 7,
        commandTriangle = 8;

    public int maxSides = 25;

    public int displaySize = 64;

    public LogicDisplay(String name){
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.displaySize, "@x@", displaySize, displaySize);
    }

    public class LogicDisplayBuild extends Building{
        public FrameBuffer buffer;
        public float color = Color.whiteFloatBits;
        public float stroke = 1f;
        public LongQueue commands = new LongQueue(256);

        @Override
        public void draw(){
            super.draw();

            Draw.draw(Draw.z(), () -> {
                if(buffer == null){
                    buffer = new FrameBuffer(displaySize, displaySize);
                    //clear the buffer - some OSs leave garbage in it
                    buffer.begin(Pal.darkerMetal);
                    buffer.end();
                }
            });

            if(!commands.isEmpty()){
                Draw.draw(Draw.z(), () -> {
                    Tmp.m1.set(Draw.proj());
                    Draw.proj(0, 0, displaySize, displaySize);
                    buffer.begin();
                    Draw.color(color);
                    Lines.stroke(stroke);

                    while(!commands.isEmpty()){
                        long c = commands.removeFirst();
                        byte type = DisplayCmd.type(c);
                        int x = DisplayCmd.x(c), y = DisplayCmd.y(c),
                        p1 = DisplayCmd.p1(c), p2 = DisplayCmd.p2(c), p3 = DisplayCmd.p3(c), p4 = DisplayCmd.p4(c);

                        switch(type){
                            case commandClear: Core.graphics.clear(x/255f, y/255f, p1/255f, 1f); break;
                            case commandLine: Lines.line(x, y, p1, p2); break;
                            case commandRect: Fill.crect(x, y, p1, p2); break;
                            case commandLineRect: Lines.rect(x, y, p1, p2); break;
                            case commandPoly: Fill.poly(x, y, Math.min(p1, maxSides), p2, p3); break;
                            case commandLinePoly: Lines.poly(x, y, Math.min(p1, maxSides), p2, p3); break;
                            case commandTriangle: Fill.tri(x, y, p1, p2, p3, p4); break;
                            case commandColor: this.color = Color.toFloatBits(x, y, p1, p2); Draw.color(this.color); break;
                            case commandStroke: this.stroke = x; Lines.stroke(x); break;
                        }
                    }

                    buffer.end();
                    Draw.proj(Tmp.m1);
                    Draw.reset();
                });
            }

            Draw.draw(Draw.z(), () -> {
                if(buffer != null){
                    Draw.rect(Draw.wrap(buffer.getTexture()), x, y, buffer.getWidth() * Draw.scl, -buffer.getHeight() * Draw.scl);
                }
            });
        }
    }

    public enum GraphicsType{
        clear,
        color,
        stroke,
        line,
        rect,
        lineRect,
        poly,
        linePoly,
        triangle;

        public static final GraphicsType[] all = values();
    }

    @Struct
    static class DisplayCmdStruct{
        public byte type;

        //9 bits are required for full 360 degrees
        @StructField(9)
        public int x, y, p1, p2, p3, p4;
    }
}
