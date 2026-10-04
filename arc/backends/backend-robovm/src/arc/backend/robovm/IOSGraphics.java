package arc.backend.robovm;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.backend.robovm.custom.*;
import arc.graphics.*;
import arc.graphics.HdpiUtils.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import com.badlogic.gdx.backends.iosrobovm.bindings.metalangle.*;
import org.robovm.apple.coregraphics.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;
import org.robovm.objc.annotation.*;
import org.robovm.rt.bro.annotation.*;

public class IOSGraphics extends Graphics{
    private static final String tag = "IOSGraphics";

    IOSApplication app;
    IOSInput input;
    IOSScreenBounds screenBounds;
    long lastFrameTime;
    float deltaTime;
    long framesStart;
    int frames;
    int fps;
    BufferFormat bufferFormat;
    String extensions;
    volatile boolean resume = false;
    volatile boolean appPaused;
    IOSApplicationConfiguration config;
    MGLContext context;
    GLVersion glVersion;
    MGLKView view;
    IOSUIViewController viewController;
    boolean firstFrame = true;
    private float ppiX;
    private float ppiY;
    private float ppcX;
    private float ppcY;
    private float density;
    private long frameId = -1;
    private boolean isContinuous = true;
    private boolean isFrameRequested = true;
    private int[] insets = new int[4];

    public IOSGraphics(IOSApplication app, IOSApplicationConfiguration config, IOSInput input){
        this.config = config;

        IOSGraphicsDelegate gdel = new IOSGraphicsDelegate();
        screenBounds = app.computeBounds();

        //HdpiUtils.setMode(config.hdpiMode);

        context = new MGLContext(MGLRenderingAPI.OpenGLES3);
        Core.glProvider = new IOSGLES30();

        view = new MGLKView(new CGRect(0, 0, screenBounds.width, screenBounds.height), context){
            @Method(selector = "touchesBegan:withEvent:")
            public void touchesBegan(@Pointer long touches, UIEvent event){
                IOSGraphics.this.input.onTouch(touches);
            }

            @Method(selector = "touchesCancelled:withEvent:")
            public void touchesCancelled(@Pointer long touches, UIEvent event){
                IOSGraphics.this.input.onTouch(touches);
            }

            @Method(selector = "touchesEnded:withEvent:")
            public void touchesEnded(@Pointer long touches, UIEvent event){
                IOSGraphics.this.input.onTouch(touches);
            }

            @Method(selector = "touchesMoved:withEvent:")
            public void touchesMoved(@Pointer long touches, UIEvent event){
                IOSGraphics.this.input.onTouch(touches);
            }

            @Override
            public void draw(CGRect rect){
                IOSGraphics.this.draw(this, rect);
            }

        };
        view.setDelegate(gdel);
        view.setDrawableColorFormat(config.colorFormat);
        view.setDrawableDepthFormat(config.depthFormat);
        view.setDrawableStencilFormat(config.stencilFormat);
        view.setDrawableMultisample(config.multisample);
        view.setMultipleTouchEnabled(true);

        viewController = new IOSUIViewController(app, this);
        viewController.setView(view);
        viewController.setDelegate(gdel);
        viewController.setPreferredFramesPerSecond(config.preferredFramesPerSecond);

        this.app = app;
        this.input = input;

        int r, g, b, a, depth, stencil = 0, samples = 0;
        if(config.colorFormat == MGLDrawableColorFormat.RGB565){
            r = 5;
            g = 6;
            b = 5;
            a = 0;
        }else{
            r = g = b = a = 8;
        }
        if(config.depthFormat == MGLDrawableDepthFormat._16){
            depth = 16;
        }else if(config.depthFormat == MGLDrawableDepthFormat._24){
            depth = 24;
        }else{
            depth = 0;
        }
        if(config.stencilFormat == MGLDrawableStencilFormat._8){
            stencil = 8;
        }
        if(config.multisample == MGLDrawableMultisample._4X){
            samples = 4;
        }
        bufferFormat = new BufferFormat(r, g, b, a, depth, stencil, samples, false);

        String machineString = HWMachine.getMachineString();
        IOSDevice device = IOSDevice.getDevice(machineString);
        if(device == null){
            Log.err(tag, "Machine ID: " + machineString + " not found, please report!");
        }else{
            Log.info(tag, "Device: " + device.classifier);
        }

        int ppi = device != null ? device.ppi : app.guessUnknownPpi();

        density = ppi / 160f;
        ppiX = ppi;
        ppiY = ppi;
        ppcX = ppiX / 2.54f;
        ppcY = ppiY / 2.54f;
        Log.info(tag, "Display: ppi=" + ppi + ", density=" + density);
        // time + FPS
        lastFrameTime = System.nanoTime();
        framesStart = lastFrameTime;
        // enable OpenGL
        makeCurrent();
        // OpenGL glViewport() function expects backbuffer coordinates instead of logical coordinates
        Core.glProvider.glViewport(0, 0, screenBounds.backBufferWidth, screenBounds.backBufferHeight);
        String versionString = Gl.getString(Gl.version);
        String vendorString = Gl.getString(Gl.vendor);
        String rendererString = Gl.getString(Gl.renderer);
        glVersion = new GLVersion(Application.ApplicationType.iOS, versionString, vendorString, rendererString);
        appPaused = false;
    }

    public void resume(){
        if(!appPaused) return;
        appPaused = false;

        Seq<ApplicationListener> listeners = app.listeners;
        synchronized(listeners){
            for(ApplicationListener listener : listeners){
                listener.resume();
            }
        }
        resume = true;
    }

    public void pause(){
        if(appPaused) return;
        appPaused = true;

        Seq<ApplicationListener> listeners = app.listeners;
        synchronized(listeners){
            for(ApplicationListener listener : listeners){
                listener.pause();
            }
        }
    }

    public void draw(MGLKView view, CGRect rect){
        makeCurrent();
        // massive hack, MGLKView resets the viewport on each draw call, so IOSGLES20
        // stores the last known viewport and we reset it here...
        Core.glProvider.glViewport(IOSGLES20.x, IOSGLES20.y, IOSGLES20.width, IOSGLES20.height);

        // For default framebuffer, we render a dummy frame during initialization before create
        // Return early so listener does not process
        if(firstFrame){
            firstFrame = false;
            return;
        }

        if(appPaused){
            return;
        }

        long time = System.nanoTime();
        if(!resume){
            deltaTime = (time - lastFrameTime) / 1000000000.0f;
        }else{
            resume = false;
            deltaTime = 0;
        }
        if(Mathf.equal(deltaTime, 0f)) deltaTime = 1f / 60f;
        lastFrameTime = time;

        frames++;
        if(time - framesStart >= 1000000000L){
            framesStart = time;
            fps = frames;
            frames = 0;
        }

        input.processEvents();
        frameId++;
        app.defaultUpdate();
        for(ApplicationListener listener : app.listeners){
            listener.update();
        }
        input.processDevices();
    }

    void makeCurrent(){
        MGLContext.setCurrentContext(context);
    }

    public void update(MGLKViewController controller){
        makeCurrent();
        app.processRunnables();
        // pause the MGLKViewController render loop if we are no longer continuous
        // and if we haven't requested a frame in the last loop iteration
        if(!isContinuous && !isFrameRequested){
            viewController.setPaused(true);
        }
        isFrameRequested = false;
    }

    protected void updateSafeInsets() {
        UIEdgeInsets edgeInsets = viewController.getView().getSafeAreaInsets();
        insets[2] = (int)edgeInsets.getTop();
        insets[0] = (int)edgeInsets.getLeft();
        insets[1] = (int)edgeInsets.getRight();
        insets[3] = (int)edgeInsets.getBottom();
        if(config.hdpiMode == HdpiMode.pixels){
            for(int i = 0; i < 4; i++){
                insets[i] *= app.pixelsPerPoint;
            }
        }
    }

    @Override
    public void setPreferredFPS(int fps){
        viewController.setPreferredFramesPerSecond(fps);
    }

    @Override
    public int[] getSafeInsets(){
        return insets;
    }

    @Override
    public int getWidth(){
        return config.hdpiMode == HdpiUtils.HdpiMode.pixels ? getBackBufferWidth() : screenBounds.width;
    }

    @Override
    public int getHeight(){
        return config.hdpiMode == HdpiUtils.HdpiMode.pixels ? getBackBufferHeight() : screenBounds.height;
    }

    @Override
    public int getBackBufferWidth(){
        return screenBounds.backBufferWidth;
    }

    @Override
    public int getBackBufferHeight(){
        return screenBounds.backBufferHeight;
    }

    @Override
    public float getDeltaTime(){
        return deltaTime;
    }

    @Override
    public int getFramesPerSecond(){
        return fps;
    }

    @Override
    public GLVersion getGLVersion(){
        return glVersion;
    }

    @Override
    public float getPpiX(){
        return ppiX;
    }

    @Override
    public float getPpiY(){
        return ppiY;
    }

    @Override
    public float getPpcX(){
        return ppcX;
    }

    @Override
    public float getPpcY(){
        return ppcY;
    }

    @Override
    public float getDensity(){
        return density;
    }

    @Override
    public void setTitle(String title){
    }

    @Override
    public void setVSync(boolean vsync){
    }

    @Override
    public BufferFormat getBufferFormat(){
        return bufferFormat;
    }

    @Override
    public boolean supportsExtension(String extension){
        if(extensions == null) extensions = Gl.getString(Gl.extensions);
        return extensions.contains(extension);
    }

    @Override
    public boolean isContinuousRendering(){
        return isContinuous;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous){
        if(isContinuous != this.isContinuous){
            this.isContinuous = isContinuous;
            // start the MGLKViewController if we go from non-continuous -> continuous
            if(isContinuous) viewController.setPaused(false);
        }
    }

    @Override
    public void requestRendering(){
        isFrameRequested = true;
        // start the MGLKViewController if we are in non-continuous mode
        // (we should already be started in continuous mode)
        if(!isContinuous) viewController.setPaused(false);
    }

    @Override
    public boolean isFullscreen(){
        return true;
    }

    @Override
    public long getFrameId(){
        return frameId;
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot){
        return null;
    }

    @Override
    public void setCursor(Cursor cursor){
    }

    @Override
    public void setSystemCursor(SystemCursor systemCursor){
    }

    class IOSGraphicsDelegate extends NSObject implements MGLKViewDelegate, MGLKViewControllerDelegate{
        @Override
        public void update(MGLKViewController MGLKViewController){
            IOSGraphics.this.update(MGLKViewController);
        }

        @Override
        public void draw(MGLKView MGLKView, CGRect cgRect){
            IOSGraphics.this.draw(MGLKView, cgRect);
        }
    }
}