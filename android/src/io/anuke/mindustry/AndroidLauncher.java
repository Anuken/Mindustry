package io.anuke.mindustry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import io.anuke.arc.Core;
import io.anuke.arc.backends.android.surfaceview.AndroidApplication;
import io.anuke.arc.backends.android.surfaceview.AndroidApplicationConfiguration;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.serialization.Base64Coder;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.ui.dialogs.FileChooser;

import java.io.*;
import java.util.ArrayList;

import static io.anuke.mindustry.Vars.*;

public class AndroidLauncher extends AndroidApplication{
    public static final int PERMISSION_REQUEST_CODE = 1;
    boolean doubleScaleTablets = true;
    FileChooser chooser;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.depth = 0;
        Platform.instance = new Platform(){

            @Override
            public void hide(){
                moveTaskToBack(true);
            }

            @Override
            public String getUUID(){
                try{
                    String s = Secure.getString(getContext().getContentResolver(), Secure.ANDROID_ID);
                    int len = s.length();
                    byte[] data = new byte[len / 2];
                    for(int i = 0; i < len; i += 2){
                        data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
                    }
                    String result = new String(Base64Coder.encode(data));
                    if(result.equals("AAAAAAAAAOA=")) throw new RuntimeException("Bad UUID.");
                    return result;
                }catch(Exception e){
                    return super.getUUID();
                }
            }

            @Override
            public void shareFile(FileHandle file){
            }

            @Override
            public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, Predicate<String> filetype){
                chooser = new FileChooser(text, file -> filetype.test(file.extension().toLowerCase()), open, cons);
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)){
                    chooser.show();
                    chooser = null;
                }else{
                    ArrayList<String> perms = new ArrayList<>();
                    if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                    if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    requestPermissions(perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                }
            }

            @Override
            public void beginForceLandscape(){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void endForceLandscape(){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }

            @Override
            public boolean canDonate(){
                return true;
            }
        };

        if(doubleScaleTablets && isTablet(this.getContext())){
            Unit.dp.addition = 0.5f;
        }

        config.hideStatusBar = true;
        Net.setClientProvider(new ArcNetClient());
        Net.setServerProvider(new ArcNetServer());
        initialize(new Mindustry(), config);
        checkFiles(getIntent());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == PERMISSION_REQUEST_CODE){
            for(int i : grantResults){
                if(i != PackageManager.PERMISSION_GRANTED) return;
            }
            if(chooser != null){
                chooser.show();
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
                Core.app.post(() -> {
                    if(save){ //open save
                        System.out.println("Opening save.");
                        FileHandle file = Core.files.local("temp-save." + saveExtension);
                        file.write(inStream, false);
                        if(SaveIO.isSaveValid(file)){
                            try{
                                SaveSlot slot = control.saves.importSave(file);
                                ui.load.runLoadSave(slot);
                            }catch(IOException e){
                                ui.showError(Core.bundle.format("save.import.fail", Strings.parseException(e, true)));
                            }
                        }else{
                            ui.showError("$save.import.invalid");
                        }
                    }else if(map){ //open map
                        FileHandle file = Core.files.local("temp-map." + mapExtension);
                        file.write(inStream, false);
                        Core.app.post(() -> {
                            System.out.println("Opening map.");
                            if(!ui.editor.isShown()){
                                ui.editor.show();
                            }
                            ui.editor.beginEditMap(file);
                        });
                    }
                });
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
