package mindustry.net;

import arc.*;
import arc.util.*;

import java.security.*;
import java.security.spec.*;

public final class NetPubKey {
    // Is not quantum resistant. But is fast. Ed25519 is more common and likely to be supported by RoboVM + Android
    private static final String keyAlgorithm = "Ed25519";

    private static PrivateKey cachedPrivateKey;
    private static PublicKey  cachedPublicKey;

    private NetPubKey(){}

    public static PublicKey getPublicKey(){
        ensureKeys();
        return cachedPublicKey;
    }

    public static byte[] getPublicKeyBytes(){
        return getPublicKey().getEncoded();
    }

    public static PublicKey decodePublicKey(byte[] encoded){
        try{
            KeyFactory kf = KeyFactory.getInstance(keyAlgorithm);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e){
            throw new IllegalArgumentException("Invalid Ed25519 public key", e);
        }
    }

    public static byte[] sign(byte[] nonce, String claimedHost){
        ensureKeys();
        try{
            byte[] payload = buildPayload(nonce, claimedHost);
            Signature signer = Signature.getInstance(keyAlgorithm);
            signer.initSign(cachedPrivateKey);
            signer.update(payload);
            return signer.sign();
        } catch (Exception e){
            throw new RuntimeException("Failed to sign auth payload", e);
        }
    }

    public static boolean verify(byte[] publicKeyBytes, byte[] nonce, String claimedHost, byte[] signature){
        try{
            PublicKey pk = decodePublicKey(publicKeyBytes);
            byte[] payload = buildPayload(nonce, claimedHost);
            Signature verifier = Signature.getInstance(keyAlgorithm);
            verifier.initVerify(pk);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (Exception e){
            // Malformed key / signature bytes, treat as generic verification failure.
            Log.warn("Verification threw exception: @", e.getMessage());
            return false;
        }
    }

    private static byte[] buildPayload(byte[] nonce, String claimedHost){
        byte[] hostBytes = claimedHost.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] payload   = new byte[nonce.length + 1 + hostBytes.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        payload[nonce.length] = ':';
        System.arraycopy(hostBytes, 0, payload, nonce.length + 1, hostBytes.length);
        return payload;
    }

    private static synchronized void ensureKeys(){
        if (cachedPrivateKey != null && cachedPublicKey != null) return;

        byte[] privBytes = Core.settings.getBytes("private-key");
        byte[] pubBytes  = Core.settings.getBytes("public-key");

        if (privBytes != null && pubBytes  != null){
            try{
                KeyFactory kf = KeyFactory.getInstance(keyAlgorithm);
                cachedPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                cachedPublicKey  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

                Log.debug("Loaded Ed25519 keypair from settings.");
                return;
            } catch (Exception e){
                Log.warn("Failed to load keypair from settings (@), regenerating.", e.getMessage());
            }
        }

        try{
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyAlgorithm);
            KeyPair kp = kpg.generateKeyPair();
            cachedPrivateKey = kp.getPrivate();
            cachedPublicKey  = kp.getPublic();

            Core.settings.put("private-key", cachedPrivateKey.getEncoded());
            Core.settings.put("public-key",  cachedPublicKey.getEncoded());

            Log.info("Generated new Ed25519 keypair and saved to settings.");
        } catch (Exception e){
            throw new RuntimeException("Failed to generate Ed25519 keypair", e);
        }
    }
}