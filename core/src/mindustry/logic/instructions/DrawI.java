package mindustry.logic.instructions;

import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.LogicDisplay.*;

public class DrawI implements LInstruction{
    public byte type;
    public LVar x, y, p1, p2, p3, p4;

    public DrawI(byte type, LVar x, LVar y, LVar p1, LVar p2, LVar p3, LVar p4){
        this.type = type;
        this.x = x;
        this.y = y;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }

    public DrawI(){
    }

    @Override
    public void run(LExecutor exec){
        //graphics on headless servers are useless.
        if(Vars.headless || exec.graphicsBuffer.size >= LExecutor.maxGraphicsBuffer) return;

        //explicitly unpack colorPack, it's pre-processed here
        if(type == LogicDisplay.commandColorPack){
            double packed = x.num();

            int value = (int)(Double.doubleToRawLongBits(packed)),
            r = ((value & 0xff000000) >>> 24),
            g = ((value & 0x00ff0000) >>> 16),
            b = ((value & 0x0000ff00) >>> 8),
            a = ((value & 0x000000ff));

            exec.graphicsBuffer.add(DisplayCmd.get(LogicDisplay.commandColor, pack(r), pack(g), pack(b), pack(a), 0, 0));
        }else if(type == LogicDisplay.commandPrint){
            CharSequence str = exec.textBuffer;

            if(str.length() > 0){
                var data = Fonts.logic.getData();
                int advance = (int)data.spaceXadvance, lineHeight = (int)data.lineHeight;

                int xOffset, yOffset;
                int align = p1.numi();

                int maxWidth = 0, lines = 1, lineWidth = 0;
                for(int i = 0; i < str.length(); i++){
                    char next = str.charAt(i);
                    if(next == '\n'){
                        maxWidth = Math.max(maxWidth, lineWidth);
                        lineWidth = 0;
                        lines++;
                    }else{
                        lineWidth++;
                    }
                }
                maxWidth = Math.max(maxWidth, lineWidth);

                float
                width = maxWidth * advance,
                height = lines * lineHeight,
                ha = ((Align.isLeft(align) ? -1f : 0f) + 1f + (Align.isRight(align) ? 1f : 0f)) / 2f,
                va = ((Align.isBottom(align) ? -1f : 0f) + 1f + (Align.isTop(align) ? 1f : 0f)) / 2f;

                xOffset = -(int)(width * ha);
                yOffset = -(int)(height * va) + (lines - 1) * lineHeight;


                int curX = x.numi(), curY = y.numi();
                for(int i = 0; i < str.length(); i++){
                    char next = str.charAt(i);
                    if(next == '\n'){
                        //move Y down when newline is encountered
                        curY -= lineHeight;
                        curX = x.numi(); //reset
                        continue;
                    }
                    if(Fonts.logic.getData().hasGlyph(next)){
                        exec.graphicsBuffer.add(DisplayCmd.get(LogicDisplay.commandPrint, packSign(curX + xOffset), packSign(curY + yOffset), next, 0, 0, 0));
                    }
                    curX += advance;

                    if(exec.graphicsBuffer.size >= LExecutor.maxGraphicsBuffer) break;
                }

                exec.textBuffer.setLength(0);
            }
        }else{
            int num1 = packSign(p1.numi()), num4 = packSign(p4.numi()), xval = packSign(x.numi()), yval = packSign(y.numi());

            if(type == LogicDisplay.commandImage){
                int packed = -1;
                if(p1.obj() instanceof UnlockableContent u){
                    packed = (u.id << 5) | (u.getContentType().ordinal() & 31);
                }else if(p1.obj() instanceof LogicDisplayBuild d){
                    packed = (d.rootDisplay.index << 5) | LogicDisplay.displayDrawType;
                }
                num1 = packed & 0x3FF;
                num4 = packed >> 10;
            }else if(type == LogicDisplay.commandScale){
                xval = packSign((int)(x.numf() / LogicDisplay.scaleStep));
                yval = packSign((int)(y.numf() / LogicDisplay.scaleStep));
            }

            //add graphics calls, cap graphics buffer size
            exec.graphicsBuffer.add(DisplayCmd.get(type, xval, yval, num1, packSign(p2.numi()), packSign(p3.numi()), num4));
        }
    }

    static int pack(int value){
        return value & 0b0111111111;
    }

    static int packSign(int value){
        return (Math.abs(value) & 0b0111111111) | (value < 0 ? 0b1000000000 : 0);
    }
}
