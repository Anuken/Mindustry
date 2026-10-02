package arc.backend.sdl;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.TaskQueue;
import arc.util.*;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import org.lwjgl.opengles.*;
import org.lwjgl.sdl.*;
import org.lwjgl.system.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import static org.lwjgl.sdl.SDLClipboard.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLHints.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLVersion.*;
import static org.lwjgl.sdl.SDLVideo.*;

public class SdlApplication implements Application{
    private final Seq<ApplicationListener> listeners = new Seq<>();
    private final TaskQueue runnables = new TaskQueue();

    final SdlGraphics graphics;
    final SdlInput input;
    final SdlConfig config;
    final Thread mainThread;

    boolean running = true;
    long window, context;

    public SdlApplication(ApplicationListener listener, SdlConfig config){
        this.config = config;
        this.listeners.add(listener);

        mainThread = Thread.currentThread();

        init();

        Core.app = this;
        Core.files = new SdlFiles();
        Core.graphics = this.graphics = new SdlGraphics(this);
        Core.input = this.input = new SdlInput();
        Core.settings = new Settings();
        Core.audio = new Audio(!config.disableAudio);

        initIcon();

        try(MemoryStack ms = MemoryStack.stackPush()){
            IntBuffer x = ms.mallocInt(1);
            IntBuffer y = ms.mallocInt(1);
            check(SDL_GetWindowSizeInPixels(window, x, y));
            graphics.updateSize(x.get(0), y.get(0));
        }

        addTextInputListener();

        try{
            loop();
            listen(ApplicationListener::exit);
        }finally{
            try{
                cleanup();
            }catch(Throwable error){
                error.printStackTrace();
            }
        }
    }

    /** Used for Scene text fields. */
    private void addTextInputListener(){
        addListener(new ApplicationListener(){
            TextField lastFocus;

            @Override
            public void update(){
                if(Core.scene != null && Core.scene.getKeyboardFocus() instanceof TextField){
                    TextField next = (TextField)Core.scene.getKeyboardFocus();
                    if(lastFocus == null){
                        SDLKeyboard.SDL_StartTextInput(window);
                    }
                    lastFocus = next;
                }else if(lastFocus != null){
                    SDLKeyboard.SDL_StopTextInput(window);
                    lastFocus = null;
                }

                if(lastFocus != null){
                    Vec2 pos = lastFocus.localToStageCoordinates(Tmp.v1.setZero());
                    try(MemoryStack stack = MemoryStack.stackPush()){
                        SDL_Rect rect = SDL_Rect.malloc(stack)
                        .set((int)pos.x,
                        Core.graphics.getHeight() - 1 - (int)(pos.y + lastFocus.getHeight()),
                        (int)lastFocus.getWidth(), (int)lastFocus.getHeight());

                        SDLKeyboard.nSDL_SetTextInputArea(window, rect.address(), 0);
                    }
                }
            }
        });
    }

    private void initIcon(){
        if(config.windowIconPaths != null && config.windowIconPaths.length > 0){
            String path = config.windowIconPaths[0];
            try{
                Pixmap p = new Pixmap(Core.files.get(path, config.windowIconFileType));
                SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(p.width, p.height, SDLPixels.SDL_PIXELFORMAT_RGBA32, p.pixels, 4 * p.width);
                if(surface != null){
                    SDL_SetWindowIcon(window, surface);
                    SDLSurface.nSDL_DestroySurface(surface.address());
                }

                p.dispose();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void init(){
        boolean useAngle = config.useAngle && OS.isWindows;

        ArcNativesLoader.load();
        NativeUtils.forceUtf8Locale();

        if(OS.isMac) restartMac();

        if(OS.isLinux && !OS.hasEnvFlag("MINDUSTRY_FORCE_WAYLAND")){
            //Prefer x11, as Wayland seems to be broken: https://github.com/Anuken/Mindustry/issues/11657
            if("wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"))){
                Log.warn("[Core] Forcing x11 due to Wayland being broken - see https://github.com/Anuken/Mindustry/issues/11657. Set MINDUSTRY_FORCE_WAYLAND=1 to disable this behavior.");
                SDL_SetHint(SDL_HINT_VIDEO_DRIVER, "x11,wayland");
            }
        }

        if(config.appName != null){
            SDL_SetAppMetadata(config.appName, config.appVersion, config.appIdentifier);
        }

        if(useAngle){
            SDL_SetHint(SDL_HINT_OPENGL_ES_DRIVER, "1");
            SDL_SetHint(SDL_HINT_VIDEO_FORCE_EGL, "1");
            ANGLELoader.load();
        }

        check(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS));

        if(useAngle){
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0);
        }

        check(SDL_SetHint(SDL_HINT_MOUSE_DPI_SCALE_CURSORS, "1"));

        if(!useAngle){
            check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, OS.isMac || config.coreProfile ? SDL_GL_CONTEXT_PROFILE_CORE : SDL_GL_CONTEXT_PROFILE_COMPATIBILITY));
        }

        check(SDL_GL_SetAttribute(SDL_GL_RED_SIZE, config.r));
        check(SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, config.g));
        check(SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, config.b));
        check(SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, config.a));
        check(SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, config.depth));
        check(SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, config.stencil));
        check(SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1));

        //this doesn't seem to do anything, but at least I tried
        if(config.samples > 0){
            check(SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, 1));
            check(SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, config.samples));
        }

        long flags = SDL_WINDOW_OPENGL;
        if(!config.initialVisible) flags |= SDL_WINDOW_HIDDEN;
        if(!config.decorated) flags |= SDL_WINDOW_BORDERLESS;
        if(config.resizable) flags |= SDL_WINDOW_RESIZABLE;
        if(config.maximized) flags |= SDL_WINDOW_MAXIMIZED;
        if(config.fullscreen) flags |= SDL_WINDOW_FULLSCREEN;

        window = SDL_CreateWindow(config.title, config.width, config.height, flags);
        if(window == 0) throw new SdlError();

        SdlError finalError = null;
        boolean createdContext = false;

        for(int[] attemptedVersion : config.glVersions){
            //windows uses GLES 3.0, don't set it here
            if(!useAngle){
                check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, attemptedVersion[0]));
                check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, attemptedVersion[1]));
            }

            try{
                context = SDL_GL_CreateContext(window);
                if(context == 0) throw new SdlError();

                createdContext = true;
                break;
            }catch(SdlError error){
                finalError = error;
                Log.err("Failed to initialize OpenGL @.@: @", attemptedVersion[0], attemptedVersion[1], Strings.getSimpleMessage(error));
                if(useAngle) break; //windows only gets one chance since it uses ANGLE, don't try other versions
            }
        }

        if(finalError != null && !createdContext) throw finalError;

        if(config.vSyncEnabled){
            check(SDL_GL_SetSwapInterval(1));
        }

        if(useAngle){
            GLES.createCapabilities();

            Core.glProvider = new SdlGLESProvider();
        }else{
            Configuration.OPENGL_EXPLICIT_INIT.set(true);
            GL.create(SDLVideo::SDL_GL_GetProcAddress);
            GL.createCapabilities();

            Core.glProvider = new SdlGLProvider();
        }

        String ver = SDL_GetRevision();

        Log.info("[Core] Initialized @ (@) / LWJGL @", ver, SDL_GetCurrentVideoDriver(), Version.getVersion());
    }

    private void loop(){

        graphics.updateSize(config.width, config.height);
        listen(ApplicationListener::init);
        try(MemoryStack stack = MemoryStack.stackPush()){
            SDL_Event event = SDL_Event.malloc(stack);

            while(running){
                while(SDLEvents.SDL_PollEvent(event)){
                    int type = event.type();
                    switch(type){
                        case SDL_EVENT_QUIT:
                            running = false;
                            break;

                        case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED:
                            int w = event.window().data1(), h = event.window().data2();
                            graphics.updateSize(w, h);
                            listen(l -> l.resize(w, h));
                            break;

                        case SDL_EVENT_WINDOW_FOCUS_GAINED:
                            listen(ApplicationListener::resume);
                            break;

                        case SDL_EVENT_WINDOW_FOCUS_LOST:
                            listen(ApplicationListener::pause);
                            break;

                        case SDL_EVENT_DROP_FILE:
                            Fi file = new Fi(event.drop().dataString());
                            listen(l -> l.fileDropped(file));
                            break;

                        default:
                            input.handleInput(event);
                    }
                }

                graphics.update();
                input.update();
                defaultUpdate();

                listen(ApplicationListener::update);

                runnables.run();

                check(SDL_GL_SwapWindow(window));
                input.postUpdate();
            }
        }
    }

    private void listen(Cons<ApplicationListener> cons){
        synchronized(listeners){
            for(ApplicationListener l : listeners){
                cons.get(l);
            }
        }
    }

    private void cleanup(){
        listen(l -> {
            l.pause();
            try{
                l.dispose();
            }catch(Throwable t){
                t.printStackTrace();
            }
        });
        dispose();

        SDL_DestroyWindow(window);
        SDL_Quit();
    }

    private void check(boolean code){
        if(!code){
            throw new SdlError();
        }
    }

    public long getWindow(){
        return window;
    }

    @Override
    public Thread getMainThread(){
        return mainThread;
    }

    @Override
    public boolean openFolder(String file){
        Threads.daemon(() -> {
            if(OS.isWindows){
                OS.execSafe("explorer.exe /select," + file.replace("/", "\\"));
            }else if(OS.isLinux){
                OS.execSafe("xdg-open", file);
            }else if(OS.isMac){
                OS.execSafe("open", file);
            }
        });
        return true;
    }

    @Override
    public boolean openURI(String url){

        //make sure it's a valid URI
        if(url.isEmpty()) return false;
        try{
            URI.create(url);
        }catch(Exception wrong){
            return false;
        }

        Threads.daemon(() -> {
            if(OS.isMac){
                OS.execSafe("open", url);
            }else if(OS.isLinux){
                OS.execSafe("xdg-open", url);
            }else if(OS.isWindows){
                OS.execSafe("rundll32", "url.dll,FileProtocolHandler", url);
            }
        });
        return true;
    }

    @Override
    public Seq<ApplicationListener> getListeners(){
        return listeners;
    }

    @Override
    public ApplicationType getType(){
        return ApplicationType.desktop;
    }

    @Override
    public String getClipboardText(){
        return SDL_GetClipboardText();
    }

    @Override
    public void setClipboardText(String text){
        SDL_SetClipboardText(text);
    }

    @Override
    public void setClipboardImage(Pixmap pixmap){
        ByteBuffer data = null;
        ByteBuffer mime = null;
        PointerBuffer mimeTypes = null;
        try{
            byte[] png = PixmapIO.writePngBytes(pixmap);

            data = MemoryUtil.memAlloc(png.length);
            data.put(png).flip();

            mime = MemoryUtil.memUTF8("image/png");
            mimeTypes = MemoryUtil.memAllocPointer(1);
            mimeTypes.put(0, mime);

            ByteBuffer fData = data;
            ByteBuffer fMime = mime;
            PointerBuffer fMimeTypes = mimeTypes;

            SDL_ClipboardDataCallbackI dataCallback = (userdata, mimeType, size) -> {
                MemoryUtil.memPutAddress(size, fData.remaining());
                return MemoryUtil.memAddress(fData);
            };

            SDL_ClipboardCleanupCallbackI cleanupCallback = userdata -> {
                MemoryUtil.memFree(fData);
                MemoryUtil.memFree(fMime);
                MemoryUtil.memFree(fMimeTypes);
            };

            if(!SDLClipboard.SDL_SetClipboardData(dataCallback, cleanupCallback, 0L, mimeTypes)){
                MemoryUtil.memFree(data);
                MemoryUtil.memFree(mime);
                MemoryUtil.memFree(mimeTypes);
            }
        }catch(Throwable ignored){
            if(data != null) MemoryUtil.memFree(data);
            if(mime != null) MemoryUtil.memFree(mime);
            if(mimeTypes != null) MemoryUtil.memFree(mimeTypes);
        }
    }

    @Override
    public void post(Runnable runnable){
        runnables.post(runnable);
    }

    @Override
    public void exit(){
        running = false;
    }

    public static class SdlError extends RuntimeException{
        public SdlError(){
            super(SDL_GetError());
        }
    }

    /** MacOS doesn't work when -XstartOnFirstThread is not passed, this will restart the program with that argument if it isn't already present. */
    @SuppressWarnings("unchecked")
    private void restartMac(){
        try{
            Class<?> mgmt = Class.forName("java.lang.management.ManagementFactory");
            Class<?> beanClass = Class.forName("java.lang.management.RuntimeMXBean");
            Object bean = Reflect.invoke(mgmt, "getRuntimeMXBean");
            String id = ((String)beanClass.getMethod("getName").invoke(bean)).split("@")[0];

            if(!OS.hasEnv("JAVA_STARTED_ON_FIRST_THREAD_" + id) || OS.env("JAVA_STARTED_ON_FIRST_THREAD_" + id).equals("0")){ //check if equal to 0 just in case
                Log.warn("Applying -XstartOnFirstThread for macOS support.");
                String javaPath = //attempt to locate java
                    new Fi(OS.prop("java.home")).child("bin/java").exists() ? new Fi(OS.prop("java.home")).child("bin/java").absolutePath() :
                    Core.files.local("jre/bin/java").exists() ? Core.files.local("jre/bin/java").absolutePath() :
                    "java";
                try{
                    Fi jar = Fi.get(SdlApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                    Seq<String> launchOptions = Seq.with(javaPath);
                    launchOptions.addAll((List<String>)beanClass.getMethod("getInputArguments").invoke(bean));
                    launchOptions.addAll(System.getProperties().entrySet().stream().map(it -> "-D" + it).toArray(String[]::new));
                    launchOptions.addAll("-XstartOnFirstThread", "-jar", jar.absolutePath(), "-firstThread");

                    Process proc = new ProcessBuilder(launchOptions.toArray(String.class)).inheritIO().start();
                    System.exit(proc.waitFor());
                }catch(IOException | URISyntaxException e){ //some part of this failed, likely failed to find java
                    Log.err(e);
                    Log.err("Failed to apply the -XstartOnFirstThread argument, it is required in order to work on mac.");
                }catch(InterruptedException ignored){}
            }
        }catch(Exception ignored){} //likely using bundled java, do nothing as the arg is already added
    }
}
