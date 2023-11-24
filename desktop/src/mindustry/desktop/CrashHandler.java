package mindustry.desktop;

import arc.func.Cons;
import arc.util.Strings;
import mindustry.net.CrashSender;

public class CrashHandler {
    /**
     * Handle game crash and display proper message to the user in dialog box
     * @param e is Throwable object
     */
    static void handleGameCrash(Throwable e) {
        Cons<Runnable> dialog = Runnable::run;
        boolean badGPU = false;
        String finalMessage = Strings.getFinalMessage(e);
        String total = Strings.getCauses(e).toString();

        if (containsAnyError(total, "Couldn't create window", "OpenGL 2.0 or higher", "pixel format", "GLEW", "unsupported combination of formats")) {
            dialog.get(() -> DesktopLauncher.message(
                    total.contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers:\n" + finalMessage :
                            "Your graphics card does not support the right OpenGL features.\n" +
                                    "Try to update your graphics drivers. If this doesn't work, your computer may not support Mindustry.\n\n" +
                                    "Full message: " + finalMessage));
            badGPU = true;
        }

        boolean fbgp = badGPU;

        CrashSender.send(e, file -> {
            Throwable fc = Strings.getFinalCause(e);
            if (!fbgp) {
                dialog.get(() -> DesktopLauncher.message("A crash has occured. It has been saved in:\n" + file.getAbsolutePath() + "\n" + fc.getClass().getSimpleName().replace("Exception", "") + (fc.getMessage() == null ? "" : ":\n" + fc.getMessage())));
            }
        });
    }

    /**
     * Checks error is present or not
     * @param source of error: String
     * @param substrings: Array of possible errors
     * @return boolean
     */
    private static boolean containsAnyError(String source, String... substrings) {
        for (String substring : substrings) {
            if (source.toLowerCase().contains(substring.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}