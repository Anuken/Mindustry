package arc.util;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.Label.*;
import arc.struct.*;

import java.io.*;
import java.text.*;
import java.util.*;

/** Records and saves GIFs. */
public class GifRecorder{
    private static final float defaultSize = 300;
    private static GifRecorder instance;

    public KeyCode
        resizeKey = KeyCode.controlLeft,
        openKey = KeyCode.e,
        recordKey = KeyCode.t,
        shiftKey = KeyCode.shiftLeft,
        switchModeKey = KeyCode.f12,
        speedMinusKey = KeyCode.minus,
        speedPlusKey = KeyCode.plus;

    public boolean outputMp4 = true;
    public Fi exportDirectory = Core.files == null ? Fi.get("gifs") : Core.files.local("gifs");
    public float speedMultiplier = 1f;
    public float[] speedModes = {0.1f, 0.25f, 0.5f, 1f, 2f, 4f, 8f};
    public int recordfps = 30;
    public float driftSpeed = 1f;
    public Rect bounds = new Rect(-defaultSize / 2, -defaultSize / 2, defaultSize, defaultSize);
    public boolean recording, open, saving;

    private float offsetx, offsety;
    private Seq<byte[]> frames = new Seq<>();
    private float frametime, saveprogress;

    public static void record(){
        if(instance == null){
            instance = new GifRecorder();
            instance.exportDirectory = Core.files.local("../../gifs");
        }
        instance.update();
    }

    /** Updates the recorder and draws the GUI */
    public void update(){
        float wx = Core.graphics.getWidth() / 2f, wy = Core.graphics.getHeight() / 2f;

        //save each frame when recording
        if(recording){
            frametime += Core.graphics.getDeltaTime() * 60.5f / speedMultiplier;
            if(frametime >= (60f / recordfps)){
                frames.add(ScreenUtils.getFrameBufferPixels(
                    (int)(bounds.x + offsetx + wx),
                    (int)(bounds.y + offsety + wy),
                    (int)bounds.width, (int)bounds.height, false
                ));
                frametime = 0;
            }
        }

        //update input
        if(Core.scene == null || !Core.scene.hasField()){
            if(Core.input.keyTap(openKey) && !saving){
                if(recording){
                    recording = false;
                    frames.clear();
                }
                open = !open;
            }

            int change = 0;
            if(Core.input.keyTap(speedMinusKey)) change --;
            if(Core.input.keyTap(speedPlusKey)) change ++;

            if(change != 0){
                int idx = 3;
                for(int i = 0; i < speedModes.length; i++){
                    if(speedModes[i] == speedMultiplier){
                        idx = i;
                        break;
                    }
                }

                speedMultiplier = speedModes[Mathf.clamp(idx + change, 0, speedModes.length - 1)];
            }

            if(Core.input.keyTap(switchModeKey) && !saving){
                outputMp4 = !outputMp4;
            }

            if(open){
                if(Core.input.keyDown(resizeKey) && !recording){
                    float xs = Mathf.round(Math.abs(wx + offsetx - Core.input.mouseX()), 2);
                    float ys = Mathf.round(Math.abs(wy + offsety - Core.input.mouseY()), 2);
                    bounds.set(-xs, -ys, xs * 2, ys * 2);
                }

                if(Core.input.keyDown(shiftKey)){
                    offsetx = Mathf.lerpDelta(offsetx, Core.input.mouseX() - wx, driftSpeed);
                    offsety = Mathf.lerpDelta(offsety, Core.input.mouseY() - wy, driftSpeed);
                }

                if(Core.input.keyTap(recordKey) && !saving){
                    if(!recording){
                        frames.clear();
                        recording = true;
                    }else{
                        recording = false;
                        saving = true;
                        saveprogress = 0f;

                        Threads.daemon(() -> {
                            if(frames.size == 0){
                                saving = false;
                                return;
                            }

                            try{
                                exportDirectory.mkdirs();
                                String outputFile = exportDirectory.absolutePath()  + "/" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date()) + (outputMp4 ? ".mp4" : ".gif");

                                String args = Strings.format(
                                "@ -r @ -s @x@ -f rawvideo -pix_fmt rgba -i - -frames:v @ -filter:v vflip@ @@",
                                OS.isLinux ? "/usr/bin/ffmpeg" : "ffmpeg",
                                recordfps, (int)bounds.width, (int)bounds.height, frames.size, outputMp4 ? "" : ",split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse",
                                (outputMp4 ? "-c:v libx264 -pix_fmt yuv420p " : ""),
                                outputFile
                                );

                                ProcessBuilder builder = new ProcessBuilder(args.split(" ")).redirectErrorStream(true);
                                Process process = builder.start();
                                OutputStream out = process.getOutputStream();

                                for(byte[] frame : frames){
                                    out.write(frame);
                                    out.flush();
                                    saveprogress += (1f / frames.size);
                                }

                                out.close();
                                process.waitFor();

                                if(OS.isLinux) Runtime.getRuntime().exec(new String[]{"bash", "-c", "echo \"file://" + outputFile + "\" | xclip -sel clip -t text/uri-list -i"});
                            }catch(Exception e){
                                Log.err(e);
                            }

                            frames.clear();
                            saving = false;
                        });
                    }
                }
            }
        }

        //draw UI
        if(open){
            Tmp.m1.set(Draw.proj());
            Draw.proj(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

            Draw.color(
            Core.input.keyDown(resizeKey) && !recording ? Color.green :
            Core.input.keyDown(shiftKey) ? Color.orange :
            recording ? Color.red :
            Color.yellow
            );

            Lines.stroke(2f);
            Lines.rect(bounds.x + wx + offsetx, bounds.y + wy + offsety, bounds.width, bounds.height);

            if(saving){
                Draw.color(Color.black);
                float w = 200, h = 50;
                Fill.crect(wx - w / 2, wy - h / 2, w, h);
                Draw.color(Color.red, Color.green, saveprogress);
                Fill.crect(wx - w / 2, wy - h / 2, w * saveprogress, h);
            }

            //attempt fetching font from several sources
            Font font = null;

            if(Core.assets != null && Core.assets.contains("outline", Font.class)){
                font = Core.assets.get("outline", Font.class);
            }

            if(font == null && Core.scene != null && Core.scene.hasStyle(LabelStyle.class)){
                font = Core.scene.getStyle(LabelStyle.class).font;
            }

            if(font != null){
                float scl = font.getData().scaleX;

                font.getData().setScale(1f);
                font.draw(
                    (int)bounds.width + "x" + (int)bounds.height + " " +
                    (saving ? "[sky][[saving " + (int)(saveprogress * 100) + "%]" : recording ? "[scarlet][[recording]" : outputMp4 ? "[coral]mp4" : "[royal]gif") +
                    (!recording && !saving ? " [gray][[" + switchModeKey + "]" : "") +
                    (speedMultiplier == 1f ? "" : "\n[white]speed: [royal]" + Strings.autoFixed(speedMultiplier, 2) + "[gray]x"),
                    bounds.x + wx + offsetx + bounds.width/2f, bounds.y + wy + offsety - 4, Align.center
                );
                font.getData().setScale(scl);
            }

            Draw.color();
            Draw.flush();
            Draw.proj(Tmp.m1);
        }
    }
}
