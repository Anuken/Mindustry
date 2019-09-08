package io.anuke.mindustry;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.Build.*;
import android.os.*;
import android.provider.Settings.*;
import android.telephony.*;
import io.anuke.arc.*;
import io.anuke.arc.backends.android.surfaceview.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.ui.dialogs.*;

import java.io.*;
import java.lang.System;

import static io.anuke.mindustry.Vars.*;

public class AndroidLauncher extends AndroidApplication{
    public static final int PERMISSION_REQUEST_CODE = 1;
    boolean doubleScaleTablets = true;
    FileChooser chooser;
    Runnable permCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        if(doubleScaleTablets && isTablet(this.getContext())){
            UnitScl.dp.addition = 0.5f;
        }
        Net.setClientProvider(new ArcNetClient());
        Net.setServerProvider(new ArcNetServer());
        initialize(new ClientLauncher(){

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
            public void showFileChooser(boolean open, String extension, Consumer<FileHandle> cons){
                if(VERSION.SDK_INT >= 19){
                    Intent intent = new Intent(open ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    addResultListener(i -> startActivityForResult(intent, i), (code, in) -> {
                        if(code == Activity.RESULT_OK && in != null && in.getData() != null){
                            Uri uri = in.getData();

                            Core.app.post(() -> Core.app.post(() -> cons.accept(new FileHandle(uri.getPath()){
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

            @Override
            public boolean canDonate(){
                return true;
            }
        }, new AndroidApplicationConfiguration(){{
            config.useImmersiveMode = true;
            config.depth = 0;
            config.hideStatusBar = true;
        }});
        checkFiles(getIntent());
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
