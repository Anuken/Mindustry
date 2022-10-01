package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LogicDisplay extends Block{
    public static final byte
        commandClear = 0,
        commandColor = 1,
        //virtual command, unpacked in instruction
        commandColorPack = 2,
        commandStroke = 3,
        commandLine = 4,
        commandRect = 5,
        commandLineRect = 6,
        commandPoly = 7,
        commandLinePoly = 8,
        commandTriangle = 9,
        commandImage = 10;

    public int maxSides = 25;

    public int displaySize = 64;
    public float scaleFactor = 1f;

    public LogicDisplay(String name){
        super(name);
        update = true;
        solid = true;
        canOverdrive = false;
        group = BlockGroup.logic;
        drawDisabled = false;
        envEnabled = Env.any;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.displaySize, "@x@", displaySize, displaySize);
    }

    public class LogicDisplayBuild extends Building{
        public FrameBuffer buffer;
        public float color = Color.whiteFloatBits;
        public float stroke = 1f;
        public LongQueue commands = new LongQueue(256);

        @Override
        public void draw(){
            super.draw();

            //don't even bother processing anything when displays are off.
            if(!Vars.renderer.drawDisplays) return;

            Draw.draw(Draw.z(), () -> {
                if(buffer == null){
                    buffer = new FrameBuffer(displaySize, displaySize);
                    //clear the buffer - some OSs leave garbage in it
                    buffer.begin(Pal.darkerMetal);
                    buffer.end();
                }
            });

            //don't bother processing commands if displays are off
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
                        int x = unpackSign(DisplayCmd.x(c)), y = unpackSign(DisplayCmd.y(c)),
                        p1 = unpackSign(DisplayCmd.p1(c)), p2 = unpackSign(DisplayCmd.p2(c)), p3 = unpackSign(DisplayCmd.p3(c)), p4 = unpackSign(DisplayCmd.p4(c));

                        switch(type){
                            case commandClear -> Core.graphics.clear(x / 255f, y / 255f, p1 / 255f, 1f);
                            case commandLine -> Lines.line(x, y, p1, p2);
                            case commandRect -> Fill.crect(x, y, p1, p2);
                            case commandLineRect -> Lines.rect(x, y, p1, p2);
                            case commandPoly -> Fill.poly(x, y, Math.min(p1, maxSides), p2, p3);
                            case commandLinePoly -> Lines.poly(x, y, Math.min(p1, maxSides), p2, p3);
                            case commandTriangle -> Fill.tri(x, y, p1, p2, p3, p4);
                            case commandColor -> Draw.color(this.color = Color.toFloatBits(x, y, p1, p2));
                            case commandStroke -> Lines.stroke(this.stroke = x);
                            case commandImage -> Draw.rect(Fonts.logicIcon(p1), x, y, p2, p2, p3);
                        }
                    }

                    buffer.end();
                    Draw.proj(Tmp.m1);
                    Draw.reset();
                });
            }

            Draw.blend(Blending.disabled);
            Draw.draw(Draw.z(), () -> {
                if(buffer != null){
                    Draw.rect(Draw.wrap(buffer.getTexture()), x, y, buffer.getWidth() * scaleFactor * Draw.scl, -buffer.getHeight() * scaleFactor * Draw.scl);
                }
            });
            Draw.blend();
        }

        @Override
        public void remove(){
            super.remove();
            if(buffer != null){
                buffer.dispose();
                buffer = null;
            }
        }
    }

    static int unpackSign(int value){
        return (value & 0b0111111111) * ((value & (0b1000000000)) != 0 ? -1 : 1);
    }

    public enum GraphicsType{
        clear,
        color,
        //virtual
        col,
        stroke,
        line,
        rect,
        lineRect,
        poly,
        linePoly,
        triangle,
        image,;

        public static final GraphicsType[] all = values();
    }

    @Struct
    static class DisplayCmdStruct{
        @StructField(4)
        public byte type;

        //at least 9 bits are required for full 360 degrees
        @StructField(10)
        public int x, y, p1, p2, p3, p4;
    }
}
