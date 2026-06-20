package mindustry.android;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.os.Build.*;
import android.telephony.*;
import arc.*;
import arc.backend.android.*;
import arc.files.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dalvik.system.*;
import mindustry.*;
import mindustry.game.Saves.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.ui.*;
import mindustry.ui.FileChooser.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.*;

import static mindustry.Vars.*;

public class AndroidLauncher extends AndroidApplication{
    public static final int PERMISSION_REQUEST_CODE = 1;
    boolean doubleScaleTablets = true;
    FileChooserDialog chooser;
    Runnable permCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            CrashHandler.log(error);

            //try to forward exception to system handler
            if(handler != null){
                handler.uncaughtException(thread, error);
            }else{
                Log.err(error);
                System.exit(1);
            }
        });

        super.onCreate(savedInstanceState);
        if(doubleScaleTablets && isTablet(this)){
            Scl.setAddition(0.5f);
        }

        initialize(new ClientLauncher(){

            @Override
            public void hide(){
                moveTaskToBack(true);
            }

            @Override
            public rhino.Context getScriptContext(){
                return AndroidRhinoContext.enter(getCacheDir());
            }

            @Override
            public void shareFile(Fi file){
            }

            @Override
            public ClassLoader loadJar(Fi jar, ClassLoader parent) throws Exception{
                //Required to load jar files in Android 14: https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
                try{
                    jar.file().setReadOnly();
                    return new DexClassLoader(jar.file().getPath(), getFilesDir().getPath(), null, parent){
                        @Override
                        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
                            //check for loaded state
                            Class<?> loadedClass = findLoadedClass(name);
                            if(loadedClass == null){
                                try{
                                    //try to load own class first
                                    loadedClass = findClass(name);
                                }catch(ClassNotFoundException | NoClassDefFoundError e){
                                    //use parent if not found
                                    return parent.loadClass(name);
                                }
                            }

                            if(resolve){
                                resolveClass(loadedClass);
                            }
                            return loadedClass;
                        }
                    };
                }catch(SecurityException e){
                    //`setReadOnly` to jar file in `/sdcard/Android/data/...` does not work on some Android 14 devices, but in `/data/...`, it does

                    if(Build.VERSION.SDK_INT < VERSION_CODES.O_MR1){
                        throw e;
                    }

                    Fi cacheDir = new Fi(getCacheDir()).child("mods");
                    cacheDir.mkdirs();

                    //long file name support
                    Fi modCacheDir = cacheDir.child(jar.nameWithoutExtension());
                    Fi modCache = modCacheDir.child(Long.toHexString(jar.lastModified()) + ".zip");

                    if(modCacheDir.equals(jar.parent())){
                        //should not reach here, just in case
                        throw e;
                    }

                    //Cache will be deleted when mod is removed
                    if(!modCache.exists() || jar.length() != modCache.length()){
                        modCacheDir.mkdirs();
                        jar.copyTo(modCache);
                    }
                    modCache.file().setReadOnly();
                    return loadJar(modCache, parent);
                }
            }

            @Override
            public void showFileChooser(FileChooserParams params){
                try{
                    String extension = params.extensions[0];

                    if(VERSION.SDK_INT >= VERSION_CODES.Q){
                        Intent intent = new Intent(params.open ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(extension.equals("zip") && !params.open && params.extensions.length == 1 ? "application/zip" : "*/*");
                        intent.putExtra(Intent.EXTRA_TITLE, params.fileName);
                        if(params.allowMultiple){
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }

                        addResultListener(i -> startActivityForResult(intent, i), (code, in) -> {
                            if(code == Activity.RESULT_OK && in != null && in.getData() != null){
                                Uri[] uris;
                                if(in.getClipData() != null){
                                    uris = new Uri[in.getClipData().getItemCount()];
                                    for(int i = 0; i < uris.length; i++){
                                        uris[i] = in.getClipData().getItemAt(i).getUri();
                                    }
                                }else{
                                    uris = new Uri[]{in.getData()};
                                }

                                if(uris.length == 0 || uris[0].getPath().contains("(invalid)")) return;

                                Fi[] files = Seq.with(uris).map(uri -> new Fi(uri.getPath()){
                                    @Override
                                    public InputStream read(){
                                        try{
                                            return getContentResolver().openInputStream(uri);
                                        }catch(IOException e){
                                            throw new ArcRuntimeException(e);
                                        }
                                    }

                                    @Override
                                    public OutputStream write(boolean append){
                                        try{
                                            return getContentResolver().openOutputStream(uri, "rwt");
                                        }catch(IOException e){
                                            throw new ArcRuntimeException(e);
                                        }
                                    }
                                }).toArray(Fi.class);

                                Core.app.post(() -> Core.app.post(() -> params.handleChooseResult(files)));
                            }
                        });
                    }else if(VERSION.SDK_INT >= VERSION_CODES.M && !(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)){
                        chooser = FileChooser.createFallbackFileChooser(params);

                        ArrayList<String> perms = new ArrayList<>();
                        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                        requestPermissions(perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                    }else{
                        FileChooser.showFallbackFileChooser(params);
                    }
                }catch(Throwable error){
                    Core.app.post(() -> Vars.ui.showException(error));
                }
            }

            @Override
            public void beginForceLandscape(){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void endForceLandscape(){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }

        }, new AndroidApplicationConfiguration(){{
            useImmersiveMode = true;
            hideStatusBar = true;
        }});
        checkFiles(getIntent());

        try{
            //new external folder
            Fi data = Core.files.absolute(((Context)this).getExternalFilesDir(null).getAbsolutePath());
            Core.settings.setDataDirectory(data);

            //delete unused cache folder to free up space
            try{
                Fi cache = Core.settings.getDataDirectory().child("cache");
                if(cache.exists()){
                    cache.deleteDirectory();
                }
            }catch(Throwable t){
                Log.err("Failed to delete cached folder", t);
            }

            //move to internal storage if there's no file indicating that it moved
            if(!Core.files.local("files_moved").exists()){
                Log.info("Moving files to external storage...");

                try{
                    //current local storage folder
                    Fi src = Core.files.absolute(Core.files.getLocalStoragePath());
                    for(Fi fi : src.list()){
                        fi.copyTo(data);
                    }
                    //create marker
                    Core.files.local("files_moved").writeString("files moved to " + data);
                    Core.files.local("files_moved_103").writeString("files moved again");
                    Log.info("Files moved.");
                }catch(Throwable t){
                    Log.err("Failed to move files!");
                    t.printStackTrace();
                }
            }
        }catch(Exception e){
            //print log but don't crash
            Log.err(e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == PERMISSION_REQUEST_CODE){
            for(int i : grantResults){
                if(i != PackageManager.PERMISSION_GRANTED) return;
            }
            if(chooser != null){
                Core.app.post(chooser::show);
            }
            if(permCallback != null){
                Core.app.post(permCallback);
                permCallback = null;
            }
        }
    }

    private void checkFiles(Intent intent){
        try{
            Uri uri = intent.getData();
            if(uri != null){
                File myFile = null;
                String scheme = uri.getScheme();
                if(scheme.equals("file")){
                    String fileName = uri.getEncodedPath();
                    myFile = new File(fileName);
                }else if(!scheme.equals("content")){
                    //error
                    return;
                }
                boolean save = uri.getPath().endsWith(saveExtension);
                boolean map = uri.getPath().endsWith(mapExtension);
                InputStream inStream;
                if(myFile != null) inStream = new FileInputStream(myFile);
                else inStream = getContentResolver().openInputStream(uri);
                Core.app.post(() -> Core.app.post(() -> {
                    if(save){ //open save
                        System.out.println("Opening save.");
                        Fi file = Core.files.local("temp-save." + saveExtension);
                        file.write(inStream, false);
                        if(SaveIO.isSaveValid(file)){
                            try{
                                SaveSlot slot = control.saves.importSave(file);
                                ui.load.runLoadSave(slot);
                            }catch(IOException e){
                                ui.showException("@save.import.fail", e);
                            }
                        }else{
                            ui.showErrorMessage("@save.import.invalid");
                        }
                    }else if(map){ //open map
                        Fi file = Core.files.local("temp-map." + mapExtension);
                        file.write(inStream, false);
                        Core.app.post(() -> {
                            System.out.println("Opening map.");
                            if(!ui.editor.isShown()){
                                ui.editor.show();
                            }
                            ui.editor.beginEditMap(file);
                        });
                    }
                }));
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private boolean isTablet(Context context){
        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager != null && manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE;
    }
}
