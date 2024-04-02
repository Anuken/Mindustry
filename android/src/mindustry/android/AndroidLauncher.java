package mindustry.android;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.Build.*;
import android.os.*;
import android.telephony.*;
import arc.*;
import arc.backend.android.*;
import arc.files.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import dalvik.system.*;
import mindustry.*;
import mindustry.game.Saves.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.*;

import static mindustry.Vars.*;
import mindustry.android.AndroidConfigurationHandler;


public class AndroidLauncher extends AndroidApplication{
    public static final int PERMISSION_REQUEST_CODE = 1;
    boolean doubleScaleTablets = true;
    FileChooser chooser;
    Runnable permCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = AndroidConfigurationHandler.configure();


        if(doubleScaleTablets && AndroidConfigurationHandler.isTablet(this)){
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
            }

            @Override
            public void showFileChooser(boolean open, String title, String extension, Cons<Fi> cons){
                showFileChooser(open, title, cons, extension);
            }

            void showFileChooser(boolean open, String title, Cons<Fi> cons, String... extensions){
                try{
                    String extension = extensions[0];

                    if(VERSION.SDK_INT >= VERSION_CODES.Q){
                        Intent intent = new Intent(open ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(extension.equals("zip") && !open && extensions.length == 1 ? "application/zip" : "*/*");

                        addResultListener(i -> startActivityForResult(intent, i), (code, in) -> {
                            if(code == Activity.RESULT_OK && in != null && in.getData() != null){
                                Uri uri = in.getData();

                                if(uri.getPath().contains("(invalid)")) return;

                                Core.app.post(() -> Core.app.post(() -> cons.get(new Fi(uri.getPath()){
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
                                            return getContentResolver().openOutputStream(uri);
                                        }catch(IOException e){
                                            throw new ArcRuntimeException(e);
                                        }
                                    }
                                })));
                            }
                        });
                    }else if(isPermissionsNotGranted()){
                        chooser = new FileChooser(title, file -> Structs.contains(extensions, file.extension().toLowerCase()), open, file -> {
                            if(!open){
                                cons.get(file.parent().child(file.nameWithoutExtension() + "." + extension));
                            }else{
                                cons.get(file);
                            }
                        });

                        ArrayList<String> perms = new ArrayList<>();
                        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                        requestPermissions(perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                    }else{
                        if(open){
                            new FileChooser(title, file -> Structs.contains(extensions, file.extension().toLowerCase()), true, cons).show();
                        }else{
                            super.showFileChooser(open, "@open", extension, cons);
                        }
                    }
                }catch(Throwable error){
                    Core.app.post(() -> Vars.ui.showException(error));
                }
            }


            private boolean isPermissionsNotGranted() {
                return VERSION.SDK_INT >= VERSION_CODES.M &&
                        !(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            }

            @Override
            public void showMultiFileChooser(Cons<Fi> cons, String... extensions){
                showFileChooser(true, "@open", cons, extensions);
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

    private void checkFiles(Intent intent) {
        try {
            Uri uri = intent.getData();
            if (uri != null) {
                File myFile = getFileFromUri(uri);
                if (myFile != null) {
                    handleFile(myFile, uri);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFileFromUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            String fileName = uri.getEncodedPath();
            return new File(fileName);
        } else if (!scheme.equals("content")) {
            return null;
        }
        return null;
    }

    private void handleFile(File file, Uri uri) throws IOException {
        boolean save = uri.getPath().endsWith(saveExtension);
        boolean map = uri.getPath().endsWith(mapExtension);
        InputStream inStream = (file != null) ? new FileInputStream(file) : getContentResolver().openInputStream(uri);
        Core.app.post(() -> Core.app.post(() -> {
            if (save) {
                handleOpenSave(file, inStream);
            } else if (map) {
                handleOpenMap(file, inStream);
            }
        }));
    }

    private void handleOpenSave(File file, InputStream inStream) {
        System.out.println("Opening save.");
        Fi saveFile = Core.files.local("temp-save." + saveExtension);
        saveFile.write(inStream, false);
        if (SaveIO.isSaveValid(saveFile)) {
            try {
                SaveSlot slot = control.saves.importSave(saveFile);
                ui.load.runLoadSave(slot);
            } catch (IOException e) {
                ui.showException("@save.import.fail", e);
            }
        } else {
            ui.showErrorMessage("@save.import.invalid");
        }
    }

    private void handleOpenMap(File file, InputStream inStream) {
        Fi mapFile = Core.files.local("temp-map." + mapExtension);
        mapFile.write(inStream, false);
        Core.app.post(() -> {
            System.out.println("Opening map.");
            if (!ui.editor.isShown()) {
                ui.editor.show();
            }
            ui.editor.beginEditMap(mapFile);
        });
    }



}
