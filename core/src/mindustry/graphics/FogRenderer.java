package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.io.SaveFileReader.*;

import java.io.*;
import java.nio.*;

import static mindustry.Vars.*;

/** Highly experimental fog-of-war renderer. */
public class FogRenderer implements CustomChunk{
    private static final float fogSpeed = 1f;
    private FrameBuffer buffer = new FrameBuffer();
    private Seq<Building> events = new Seq<>();
    private boolean read = false;
    private Rect rect = new Rect();

    public FogRenderer(){
        SaveVersion.addCustomChunk("fog", this);

        Events.on(WorldLoadEvent.class, event -> {
            if(state.rules.fog){
                buffer.resize(world.width(), world.height());

                events.clear();
                Groups.build.copy(events);

                //clear
                if(!read){
                    buffer.begin(Color.black);
                    buffer.end();
                }

                read = false;
            }
        });

        //draw fog when tile is placed.
        Events.on(TileChangeEvent.class, event -> {
            if(state.rules.fog && event.tile.build != null && event.tile.isCenter()){
                events.add(event.tile.build);
            }
        });
    }

    public Texture getTexture(){
        return buffer.getTexture();
    }

    public void drawFog(){
        //resize if world size changes
        boolean clear = buffer.resizeCheck(world.width(), world.height());

        //set projection to whole map
        Draw.proj(0, 0, buffer.getWidth() * tilesize, buffer.getHeight() * tilesize);

        //if the buffer resized, it contains garbage now, clear it.
        if(clear){
            buffer.begin(Color.black);
        }else{
            buffer.begin();
        }

        Gl.blendEquationSeparate(Gl.max, Gl.max);
        ScissorStack.push(rect.set(1, 1, buffer.getWidth() - 2, buffer.getHeight() - 2));

        Draw.color(Color.white);

        for(var build : events){
            if(build.team == player.team()){
                Fill.circle(build.x, build.y, 40f);
            }
        }
        events.clear();

        Draw.alpha(fogSpeed * Math.max(Time.delta, 1f));

        //TODO slow and terrible
        Groups.unit.each(u -> {
            if(u.team == player.team()){
                Fill.circle(u.x, u.y, u.type.lightRadius * 1.5f);
            }
        });

        buffer.end();
        buffer.getTexture().setFilter(TextureFilter.linear);
        Gl.blendEquationSeparate(Gl.funcAdd, Gl.funcAdd);
        ScissorStack.pop();

        Draw.proj(Core.camera);

        Draw.shader(Shaders.fog);
        Draw.fbo(buffer.getTexture(), world.width(), world.height(), tilesize);
        Draw.shader();
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        ByteBuffer bytes = Buffers.newUnsafeByteBuffer(buffer.getWidth() * buffer.getHeight() * 4);
        try{
            bytes.position(0);
            buffer.begin();
            Gl.readPixels(0, 0, buffer.getWidth(), buffer.getHeight(), Gl.rgba, Gl.unsignedByte, bytes);
            buffer.end();
            bytes.position(0);
            stream.writeShort(buffer.getWidth());
            stream.writeShort(buffer.getHeight());

            //TODO flip?

            int pos = 0, size = bytes.capacity() / 4;
            while(pos < size){
                int consecutives = 0;
                boolean cur = bytes.get(pos * 4) != 0;
                while(consecutives < 127 && pos < size){
                    boolean next = bytes.get(pos * 4) != 0;
                    if(cur != next){
                        break;
                    }

                    consecutives ++;
                    pos ++;
                }
                int mask = (cur ? 0b1000_0000 : 0);
                stream.write(mask | (consecutives));
            }
        }finally{
            Buffers.disposeUnsafeByteBuffer(bytes);
        }
    }

    @Override
    public void read(DataInput stream) throws IOException{
        short w = stream.readShort(), h = stream.readShort();
        int pos = 0;
        int len = w * h;
        buffer.resize(w, h);
        buffer.begin(Color.black);
        Draw.proj(0, 0, buffer.getWidth(), buffer.getHeight());
        Draw.color();

        while(pos < len){
            int data = stream.readByte() & 0xff;
            boolean sign = (data & 0b1000_0000) != 0;
            int consec = data & 0b0111_1111;

            if(sign){
                for(int i = 0; i < consec; i++){
                    int x = pos % w, y = pos / w;
                    //TODO this is slow
                    Fill.rect(x + 0.5f, y + 0.5f, 1f, 1f);

                    pos ++;
                }
            }else{
                pos += consec;
            }
        }

        buffer.end();
        Draw.proj(Core.camera);
        read = true;
    }

    @Override
    public boolean shouldWrite(){
        return state.rules.fog && buffer.getTexture() != null && buffer.getWidth() > 0;
    }
}
