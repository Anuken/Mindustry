package mindustry.game;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Rewind {
    public static void main(String[] args) {
        try {
            String jarPath = "core/src/mindustry/game/v63.jar";

            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println("OUTPUT: " + line);
                }

                while ((line = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Other .jar exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
