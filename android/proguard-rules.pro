-dontobfuscate

-keep class mindustry.** { *; }
-keep class arc.** { *; }
-keep class net.jpountz.** { *; }
-keep class rhino.** { *; }
-keep class com.android.dex.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-dontwarn javax.naming.**

#-printusage out.txt