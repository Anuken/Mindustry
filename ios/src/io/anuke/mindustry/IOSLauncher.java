package io.anuke.mindustry;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.TextField;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        Platform.instance = new Platform() {
            DateFormat format = SimpleDateFormat.getDateTimeInstance();

            @Override
            public String format(Date date) {
                return format.format(date);
            }

            @Override
            public String format(int number) {
                return NumberFormat.getIntegerInstance().format(number);
            }

            @Override
            public void addDialog(TextField field) {
                TextFieldDialogListener.add(field, 16);
            }

            @Override
            public void addDialog(TextField field, int maxLength) {
                TextFieldDialogListener.add(field, maxLength);
            }

            @Override
            public String getLocaleName(Locale locale) {
                return locale.getDisplayName(locale);
            }
        };

        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new Mindustry(), config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}