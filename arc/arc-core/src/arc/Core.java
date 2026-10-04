package arc;

import arc.assets.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.scene.*;
import arc.util.*;

import java.util.concurrent.*;

/** Global references to all of Arc's core modules. */
public class Core{
    public static Application app;
    public static Graphics graphics;
    public static Audio audio;
    public static Input input;
    public static Files files;
    public static Settings settings;

    public static I18NBundle bundle = I18NBundle.createEmptyBundle();
    public static Camera camera;
    public static Batch batch;
    public static Scene scene;
    public static AssetManager assets;
    public static TextureAtlas atlas;
    public static ExecutorService executor = Threads.executor("Main Executor", OS.cores);

    /** This class should never be used directly - use {@link Gl} instead. */
    public static GLProvider glProvider;
}
