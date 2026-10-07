package arc.graphics.gl;

import arc.Application.*;
import arc.util.*;

import java.util.*;
import java.util.regex.*;

import static arc.Application.ApplicationType.*;

public class GLVersion{
    public final String vendorString;
    public final String rendererString;
    public final String versionString;
    public final GlType type;
    public int majorVersion;
    public int minorVersion;
    public int releaseVersion;

    public GLVersion(ApplicationType appType, String versionString, String vendorString, String rendererString){
        if(appType == android || appType == iOS || versionString.toLowerCase(Locale.ROOT).startsWith("opengl es")) this.type = GlType.GLES;
        else if(appType == desktop) this.type = GlType.OpenGL;
        else if(appType == web) this.type = GlType.WebGL;
        else this.type = GlType.NONE;

        if(type == GlType.GLES){
            //OpenGL<space>ES<space><version number><space><vendor-specific information>.
            extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString);
        }else if(type == GlType.WebGL){
            //WebGL<space><version number><space><vendor-specific information>
            extractVersion("WebGL (\\d(\\.\\d){0,2})", versionString);
        }else if(type == GlType.OpenGL){
            //<version number><space><vendor-specific information>
            extractVersion("(\\d(\\.\\d){0,2})", versionString);
        }else{
            majorVersion = -1;
            minorVersion = -1;
            releaseVersion = -1;
            vendorString = "";
            rendererString = "";
        }

        this.versionString = versionString;
        this.vendorString = vendorString;
        this.rendererString = rendererString;
    }

    private void extractVersion(String patternString, String versionString){
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(versionString);
        boolean found = matcher.find();
        if(found){
            String result = matcher.group(1);
            String[] resultSplit = result.split("\\.");
            majorVersion = parseInt(resultSplit[0], 2);
            minorVersion = resultSplit.length < 2 ? 0 : parseInt(resultSplit[1], 0);
            releaseVersion = resultSplit.length < 3 ? 0 : parseInt(resultSplit[2], 0);
        }else{
            Log.err("[Arc GL] Invalid version string: " + versionString);
            majorVersion = 2;
            minorVersion = 0;
            releaseVersion = 0;
        }
    }

    /** Forgiving parsing of gl major, minor and release versions as some manufacturers don't adhere to spec **/
    private int parseInt(String v, int defaultValue){
        try{
            return Integer.parseInt(v);
        }catch(NumberFormatException nfe){
            Log.err("[Arc GL] Error parsing number: " + v + ", assuming: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Checks to see if the current GL connection version is higher, or equal to the provided test versions.
     * @param testMajorVersion the major version to test against
     * @param testMinorVersion the minor version to test against
     * @return true if the current version is higher or equal to the test version
     */
    public boolean atLeast(int testMajorVersion, int testMinorVersion){
        return majorVersion > testMajorVersion || (majorVersion == testMajorVersion && minorVersion >= testMinorVersion);
    }

    /** @return a string with the current GL connection data */
    public String getDebugVersionString(){
        return "Type: " + type + "\n" +
        "Version: " + majorVersion + ":" + minorVersion + ":" + releaseVersion + "\n" +
        "Vendor: " + vendorString + "\n" +
        "Renderer: " + rendererString;
    }

    @Override
    public String toString(){
        return type + " " + majorVersion + "." + minorVersion + "." + releaseVersion + " / " + vendorString + " / " + rendererString + " / " + versionString;
    }

    public enum GlType{
        OpenGL,
        GLES,
        WebGL,
        NONE
    }
}
