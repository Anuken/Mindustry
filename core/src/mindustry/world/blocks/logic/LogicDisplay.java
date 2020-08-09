package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.*;

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
        commandFlush = 8;

    public int displaySize = 64;

    public LogicDisplay(String name){
        super(name);
        update = true;
    }

    public class LogicDisplayEntity extends Building{
        public FrameBuffer buffer;
        public float color = Color.whiteFloatBits;
        public float stroke = 1f;
        public LongQueue commands = new LongQueue();

        public LogicDisplayEntity(){

        }

        @Override
        public void draw(){
            super.draw();

            if(buffer == null){
                buffer = new FrameBuffer(displaySize, displaySize);
            }

            if(!commands.isEmpty()){
                Draw.draw(Draw.z(), () -> {
                    Tmp.m1.set(Draw.proj());
                    Draw.proj(0, 0, displaySize, displaySize);
                    buffer.begin();
                    Draw.color(color);
                    Lines.stroke(stroke);
                    Lines.precise(true);

                    while(!commands.isEmpty()){
                        long c = commands.removeFirst();
                        byte type = DisplayCmd.type(c);
                        int x = DisplayCmd.x(c), y = DisplayCmd.y(c),
                        p1 = DisplayCmd.p1(c), p2 = DisplayCmd.p2(c), p3 = DisplayCmd.p3(c);

                        switch(type){
                            case commandClear: Core.graphics.clear(x/255f, y/255f, p1/255f, 1f); break;
                            case commandLine: Lines.line(x, y, p1, p2); break;
                            case commandRect: Fill.crect(x, y, p1, p2); break;
                            case commandLineRect: Lines.rect(x, y, p1, p2); break;
                            case commandPoly: Fill.poly(x, y, p1, p2, p3); break;
                            case commandLinePoly: Lines.poly(x, y, p1, p2, p3); break;
                            case commandColor: this.color = Color.toFloatBits(x, y, p1, 255); Draw.color(this.color); break;
                            case commandStroke: this.stroke = x; Lines.stroke(x); break;
                        }
                    }

                    Lines.precise(false);

                    buffer.end();
                    Draw.proj(Tmp.m1);
                    Draw.reset();
                });
            }

            Draw.rect(Draw.wrap(buffer.getTexture()), x, y, buffer.getWidth() * Draw.scl, -buffer.getHeight() * Draw.scl);
        }
    }

    public enum CommandType{
        clear,
        color,
        stroke,
        line,
        rect,
        lineRect,
        poly,
        linePoly,
        flush;

        public static final CommandType[] all = values();
        public static final CommandType[] allNormal = Seq.select(all, t -> t != flush).toArray(CommandType.class);
    }

    @Struct
    static class DisplayCmdStruct{
        public byte type;

        //each coordinate is only 8 bits, limiting size to 256x256
        //anything larger than that would be excessive anyway
        @StructField(9)
        public int x, y, p1, p2, p3;
    }
}
