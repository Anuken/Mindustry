package mindustry.net;

import arc.struct.ObjectMap;
import mindustry.annotations.Annotations.*;
import arc.Core;
import arc.Net.HttpMethod;
import arc.Net.HttpRequest;
import arc.func.Cons;
import arc.math.Rand;
import arc.util.Log;
import arc.util.NetJavaImpl;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.ValueType;
import arc.util.serialization.JsonWriter.OutputType;
import mindustry.gen.Call;
import mindustry.gen.Playerc;
import mindustry.net.Administration.Config;
import mindustry.net.Packets.KickReason;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static mindustry.Vars.*;

public class Authentication{
    public static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    /** Represents a response from the api */
    public static class ApiResponse<T>{
        /** Whether or not the request is done */
        public boolean finished;
        /** Was the request successful? */
        public boolean success;
        /** Error code from api */
        public String errorCode;
        /** Error description from api */
        public String errorDescription;
        /** Locally thrown exception if request was unsuccessful */
        public Throwable exception;
        /** Result of the request */
        public T result;
        /** Callback on success or error */
        public Cons<ApiResponse<T>> callback;

        public void setResult(T result){
            finished = true;
            success = true;
            this.result = result;
            if(callback != null) Core.app.post(() -> callback.get(this));
        }

        public void setError(String code, String description){
            finished = true;
            success = false;
            this.errorCode = code;
            this.errorDescription = description;
            if(callback != null) Core.app.post(() -> callback.get(this));
        }

        public void setError(Throwable ex){
            finished = true;
            success = false;
            exception = ex;
            if(callback != null) Core.app.post(() -> callback.get(this));
        }

        // should only be called once
        public void done(Cons<ApiResponse<T>> cb){
            if(finished){
                cb.get(this);
                return;
            }
            callback = cb;
        }

        public JsonValue tryParseResponse(String data) {
            if(data == null){
                setError(new RuntimeException("Expected the api to produce a response"));
                return null;
            }
            JsonValue parsed;
            try {
                parsed = new JsonReader().parse(data);
            } catch (SerializationException ex) {
                setError(new RuntimeException("Invalid response from api", ex));
                return null;
            }
            if(!parsed.isObject()){
                setError(new RuntimeException("Expected api response to be a json object"));
                return null;
            }
            try {
                String status = parsed.getString("status");
                if(!status.equals("ok")){
                    setError(parsed.getString("error"), parsed.getString("description"));
                    return null;
                }
                return parsed;
            }catch(IllegalArgumentException ex){
                setError(new RuntimeException("Invalid response from api", ex));
                return null;
            }
        }
    }

    /** Represents information used when asking user for credentials */
    public static class LoginInfo{
        // values provided by server
        public String authServer;
        public boolean showRegister;
        public String loginNotice;

        // values provided by user
        public String username;
        public String password;

        public Runnable successCallback;
    }

    @Serialize
    public static class Session{
        public String username;
        public String token;
        public long expires;

        public Session(){}
        public Session(String username, String token, long expires){
            this.username = username;
            this.token = token;
            this.expires = expires;
        }
    }

    public enum AuthResult{
        succeeded, invalidToken, ipMismatch, serverMismatch, usernameMismatch, serverFail;

        @Override
        public String toString(){
            return Core.bundle.get("login.fail." + name());
        }
    }

    public NetJavaImpl netImpl = new NetJavaImpl();
    public LoginInfo loginInfo;
    public ObjectMap<String, Session> sessions = new ObjectMap<>();

    public Authentication(){
        load();
    }

    @Remote(targets = Loc.server, priority = PacketPriority.high, variants = Variant.one)
    public static void authenticationRequired(String authServer, String serverIdHash){
        // do not allow server to request authentication twice
        if(netClient.authenticationRequested) return;
        netClient.authenticationRequested = true;

        netClient.authenticating = true;
        ui.loadfrag.show("$connecting.auth");
        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        // callback hell and overall very ugly code
        auth.doConnect(authServer, serverIdHash).done(response -> {
            if(response.success){
                netClient.authenticating = false;
                // doConnect will check if the session exists, NPE shouldn't happen here
                Call.sendAuthenticationResponse(auth.sessions.get(authServer).username, response.result);
                return;
            }
            if("INVALID_SESSION".equals(response.errorCode)){
                auth.tryLogin(authServer, () -> {
                    ui.loadfrag.show("$connecting.auth");
                    ui.loadfrag.setButton(() -> {
                        ui.loadfrag.hide();
                        netClient.disconnectQuietly();
                    });
                    auth.doConnect(authServer, serverIdHash).done(response2 -> {
                        if(response2.success){
                            netClient.authenticating = false;
                            Call.sendAuthenticationResponse(auth.sessions.get(authServer).username, response2.result);
                            return;
                        }
                        disconnectAndShowApiError(response2);
                    });
                });
            }else{
                // unexpected error
                disconnectAndShowApiError(response);
            }
        });
    }

    @Remote(targets = Loc.client, priority = PacketPriority.high)
    public static void sendAuthenticationResponse(Playerc player, String username, String token){
        if(player.con() == null) return;
        if(!player.con().authenticationRequested) return;
        player.con().authenticationRequested = false;
        if(username == null || token == null){
            player.con().kick(KickReason.authenticationFailed);
            return;
        }

        String address = Config.authVerifyIP.bool() ? player.con().address : null;
        auth.verifyConnect(username, token, address).done(response -> {
            if(response.success) {
                player.con().authenticated = true;
                player.username(username);
                Call.sendAuthenticationResult(player.con(), AuthResult.succeeded);
                netServer.finalizeConnect(player);
                Log.info("Authentication succeeded for player &lc{0}&lg (username &lc{1}&lg)", player.name(), username);
            }else{
                if(response.exception != null){
                    Log.err("Unexpected error in authentication", response.exception);
                    Call.sendAuthenticationResult(player.con(), AuthResult.serverFail);
                }else{
                    Log.info("Player &lc{0}&lg failed authentication: {1} ({2})", player.name(), response.errorCode, response.errorDescription);
                    AuthResult result = AuthResult.serverFail;
                    switch(response.errorCode){
                        case "NO_SUCH_TOKEN":
                        case "TOKEN_EXPIRED":
                            result = AuthResult.invalidToken;
                            break;
                        case "USERNAME_MISMATCH":
                            result = AuthResult.usernameMismatch;
                            break;
                        case "IP_MISMATCH":
                            result = AuthResult.ipMismatch;
                            break;
                        case "SERVER_ID_MISMATCH":
                            result = AuthResult.serverMismatch;
                            break;
                    }
                    Call.sendAuthenticationResult(player.con(), result);
                }
                player.con().kick(KickReason.authenticationFailed);
            }
        });
    }

    @Remote(targets = Loc.server, priority = PacketPriority.high, variants = Variant.one)
    public static void sendAuthenticationResult(AuthResult result){
        if(result == AuthResult.succeeded){
            ui.loadfrag.show("$connecting.data");
            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                netClient.disconnectQuietly();
            });
        }else {
            ui.showText("$login.fail", result.toString());
        }
    }

    public static void disconnectAndShowApiError(ApiResponse<?> response){
        netClient.disconnectQuietly();
        ui.loadfrag.hide();
        showApiError(response);
    }

    public static void showApiError(ApiResponse<?> response){
        if(response.exception != null){
            Log.err("Unexpected exception while connecting to authentication server", response.exception);
            ui.showException("$login.error", response.exception);
        }
        if(response.errorCode != null){
            Log.err("Unexpected error from authentication server: " + response.errorCode + " (" + response.errorDescription + ")");
            ui.showErrorMessage(Core.bundle.format("login.error") + "\n\n" +
                    response.errorCode + " (" + response.errorDescription + ")");
        }
    }

    public static String sha256(byte[] input){
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-256");
        }catch(NoSuchAlgorithmException ex){
            throw new RuntimeException(ex);
        }
        byte[] hashed = digest.digest(input);
        char[] hex = new char[hashed.length * 2];
        for (int i = 0; i < hashed.length; i++) {
            int val = hashed[i] & 0xff; // convert to unsigned
            hex[i * 2] = HEX_DIGITS[val >>> 4];
            hex[i * 2 + 1] = HEX_DIGITS[val & 0x0f];
        }
        return new String(hex);
    }

    public boolean enabled(){
        return Config.authEnabled.bool();
    }

    public String getServerId(){
        String id = Core.settings.getString("authentication-server-id", null);
        if(id != null) return id;

        byte[] bytes = new byte[12];
        new Rand().nextBytes(bytes);
        String result = new String(Base64Coder.encode(bytes));
        Core.settings.put("authentication-server-id", result);
        String hashed = sha256(bytes);
        Core.settings.put("authentication-server-id-hash", hashed);
        Core.settings.save();
        return result;
    }

    public String getServerIdHash(){
        String hash = Core.settings.getString("authentication-server-id-hash", null);
        if(hash != null) return hash;
        String serverId = getServerId();
        // no reason to calculate the hash twice
        hash = Core.settings.getString("authentication-server-id-hash", null);
        if(hash != null) return hash;
        String hashed = sha256(Base64Coder.decode(serverId));
        Core.settings.putSave("authentication-server-id-hash", hashed);
        return hashed;
    }

    public void save(){
        Core.settings.putObject("authentication-sessions", sessions);
        Core.settings.save();
    }

    @SuppressWarnings("unchecked")
    public void load(){
        sessions = Core.settings.getObject("authentication-sessions", ObjectMap.class, ObjectMap::new);
    }

    public void handleConnect(Playerc player){
        String authServer = Config.authServer.string();
        if(authServer == null){
            Log.err("Authentication was enabled but no authentication server was specified!");
            player.con().kick(KickReason.authenticationFailed);
            return;
        }

        player.con().authenticationRequested = true;
        Call.authenticationRequired(player.con(), authServer, getServerIdHash());
    }

    public String buildUrl(String base, String endpoint) {
        if(base.endsWith("/")){
            base = base.substring(0, base.length() - 1);
        }
        return base + endpoint;
    }

    public ApiResponse<LoginInfo> fetchServerInfo(String authServer){
        HttpRequest request = new HttpRequest();
        request.method(HttpMethod.GET);
        request.url(buildUrl(authServer, "/api/info"));
        ApiResponse<LoginInfo> response = new ApiResponse<>();
        netImpl.http(request, res -> {
            JsonValue obj = response.tryParseResponse(res.getResultAsString());
            if(obj == null) return;
            LoginInfo info = new LoginInfo();
            info.authServer = authServer;
            try{
                info.showRegister = obj.getBoolean("registrationEnabled");
                info.loginNotice = obj.getString("loginNotice", null);
            }catch(IllegalArgumentException ex){
                response.setError(new RuntimeException("Invalid response from api", ex));
                return;
            }
            response.setResult(info);
        }, response::setError);
        return response;
    }

    public ApiResponse<Session> doLogin(String authServer, String username, String password){
        JsonValue body = new JsonValue(ValueType.object);
        body.addChild("username", new JsonValue(username));
        body.addChild("password", new JsonValue(password));
        HttpRequest request = new HttpRequest();
        request.method(HttpMethod.POST);
        request.url(buildUrl(authServer, "/api/login"));
        request.header("Content-Type", "application/json");
        request.content(body.toJson(OutputType.json));
        ApiResponse<Session> response = new ApiResponse<>();
        netImpl.http(request, res -> {
            JsonValue obj = response.tryParseResponse(res.getResultAsString());
            if(obj == null) return;
            Session session = new Session();
            try{
                // case-corrected username
                session.username = obj.getString("username");
                session.token = obj.getString("token");
                session.expires = obj.getLong("expiry");
            }catch(IllegalArgumentException ex){
                response.setError(new RuntimeException("Invalid response from api"));
                return;
            }
            sessions.put(authServer, session);
            save();
            response.setResult(session);
        }, response::setError);
        return response;
    }

    public ApiResponse<String> doConnect(String authServer, String serverHash){
        ApiResponse<String> response = new ApiResponse<>();
        Session session = sessions.get(authServer);
        if(session == null){
            response.setError("INVALID_SESSION", "session token not found");
            return response;
        }
        JsonValue body = new JsonValue(ValueType.object);
        body.addChild("serverHash", new JsonValue(serverHash));
        HttpRequest request = new HttpRequest();
        request.method(HttpMethod.POST);
        request.url(buildUrl(authServer, "/api/doconnect"));
        request.header("Content-Type", "application/json");
        request.header("Session", session.token);
        request.content(body.toJson(OutputType.json));
        netImpl.http(request, res -> {
            JsonValue obj = response.tryParseResponse(res.getResultAsString());
            if(obj == null) return;
            String connectToken;
            try{
                connectToken = obj.getString("token");
            }catch(IllegalArgumentException ex){
                response.setError(new RuntimeException("Invalid response from api", ex));
                return;
            }
            response.setResult(connectToken);
        }, response::setError);
        return response;
    }

    public ApiResponse<Boolean> verifyConnect(String username, String token, String ip){
        JsonValue body = new JsonValue(ValueType.object);
        body.addChild("serverId", new JsonValue(getServerId()));
        body.addChild("username", new JsonValue(username));
        body.addChild("token", new JsonValue(token));
        if(ip != null) body.addChild("ip", new JsonValue(ip));
        HttpRequest request = new HttpRequest();
        request.method(HttpMethod.POST);
        request.url(buildUrl(Config.authServer.string(), "/api/verifyconnect"));
        request.header("Content-Type", "application/json");
        request.content(body.toJson(OutputType.json));
        ApiResponse<Boolean> response = new ApiResponse<>();
        netImpl.http(request, res -> {
            JsonValue obj = response.tryParseResponse(res.getResultAsString());
            if(obj == null) return;
            response.setResult(true);
        }, response::setError);
        return response;
    }

    public void tryLogin(String authServer, Runnable onSuccess){
        Log.info("Fetching authentication server information for {0}", authServer);
        fetchServerInfo(authServer).done(response -> {
            if(response.success){
                loginInfo = response.result;
                loginInfo.successCallback = onSuccess;
                ui.loadfrag.hide();
                ui.login.show();
                return;
            }
            disconnectAndShowApiError(response);
        });
    }
}
