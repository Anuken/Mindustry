import mindustry.*;
import org.codehaus.mojo.animal_sniffer.*;
import org.codehaus.mojo.animal_sniffer.logging.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;

public class AndroidApiTests{

    static final Pattern covariantBufferCall = Pattern.compile("Undefined reference: java\\.nio\\.(\\w+Buffer) java\\.nio\\.\\1\\.(position|limit|flip|clear|mark|reset|rewind)\\(");

    static class CollectingLogger implements Logger{
        final List<String> errors = new ArrayList<>();

        @Override public void info(String message){}
        @Override public void info(String message, Throwable t){}
        @Override public void debug(String message){}
        @Override public void debug(String message, Throwable t){}
        @Override public void warn(String message){}
        @Override public void warn(String message, Throwable t){}
        @Override public void error(String message){ errors.add(message); }
        @Override public void error(String message, Throwable t){ errors.add(message); }
    }

    @Test
    void coreOnlyUsesAndroidApi() throws Exception{
        String signaturePath = System.getProperty("android.signature");
        assertNotNull(signaturePath, "android.signature system property must point to the Android API signature file.");

        CollectingLogger logger = new CollectingLogger();

        Set<String> knownClasses = new HashSet<>();
        ClassListBuilder builder = new ClassListBuilder(knownClasses, logger);
        for(String entry : System.getProperty("java.class.path").split(File.pathSeparator)){
            builder.process(new File(entry));
        }

        File core = new File(Vars.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        SignatureChecker checker;
        try(InputStream signature = new FileInputStream(signaturePath)){
            checker = new SignatureChecker(signature, knownClasses, logger);
        }
        checker.setSourcePath(List.of());
        checker.setAnnotationTypes(List.of("mindustry.annotations.Annotations$IgnoreAndroidApi"));
        checker.process(core);

        List<String> violations = logger.errors.stream().filter(error -> !covariantBufferCall.matcher(error).find()).toList();

        assertTrue(violations.isEmpty(), "core uses APIs that are not available on Android:\n" + String.join("\n", violations));
    }
}
