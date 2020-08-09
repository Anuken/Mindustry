package mindustry.android;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.Build.*;
import android.os.*;
import android.provider.Settings.*;
import android.telephony.*;
import arc.*;
import arc.backend.android.*;
import arc.files.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.game.Saves.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.lang.System;
import java.lang.Thread.*;
import java.util.*;

import static mindustry.Vars.*;


public class AndroidLauncher extends AndroidApplication{
    public static final int PERMISSION_REQUEST_CODE = 1;
    boolean doubleScaleTablets = true;
    FileChooser chooser;
    Runnable permCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            CrashSender.log(error);

            //try to forward exception to system handler
            if(handler != null){
                handler.uncaughtException(thread, error);
            }else{
                error.printStackTrace();
                System.exit(1);
            }
        });

        super.onCreate(savedInstanceState);
        if(doubleScaleTablets && isTablet(this.getContext())){
            Scl.setAddition(0.5f);
        }

        initialize(new ClientLauncher(){

            @Override
            public void hide(){
                moveTaskToBack(true);
            }

            @Override
            public rhino.Context getScriptContext(){
                return AndroidRhinoContext.enter(getContext().getCacheDir());
            }

            @Override
            public void shareFile(Fi file){
            }

            @Override
            public void showFileChooser(boolean open, String extension, Cons<Fi> cons){
                if(VERSION.SDK_INT >= VERSION_CODES.Q){
                    Intent intent = new Intent(open ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(extension.equals("zip") && !open ? "application/zip" : "*/*");
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
                }else if(VERSION.SDK_INT >= VERSION_CODES.M && !(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)){
                    chooser = new FileChooser(open ? "@open" : "@save", file -> file.extension().equalsIgnoreCase(extension), open, file -> {
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
                    super.showFileChooser(open, extension, cons);
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
            stencil = 8;
        }});
        checkFiles(getIntent());


        //new external folder
        Fi data = Core.files.absolute(getContext().getExternalFilesDir(null).getAbsolutePath());
        Core.settings.setDataDirectory(data);

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
