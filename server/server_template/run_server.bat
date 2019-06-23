@Echo Off
Set "JV="
For /F "Tokens=3" %%A In ('java -version 2^>^&1') Do If Not Defined JV Set "JV=%%~A"
If /I "%JV%"=="not" (Echo Java is not installed, this server requires Java to run.) Else (start java -jar server.jar)
Pause
