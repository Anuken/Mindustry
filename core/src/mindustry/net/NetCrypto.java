package mindustry.net;

import arc.*;
import arc.util.*;

import java.security.*;
import java.security.spec.*;

/**
 * Handles Ed25519 signing and verification for player verification.
 * <p>
 * The server generates a keypair on first run and saves it to settings. The public key is sent to clients
 * during the handshake, and clients use it to verify that the player is who it claims to be.
 */
public final class NetCrypto {
    private static final String ALGORITHM = "Ed25519";

    // null means not yet loaded
    private static PrivateKey cachedPrivateKey;
    private static PublicKey  cachedPublicKey;

    private NetCrypto() {}

    public static PublicKey getPublicKey() {
        ensureKeys();
        return cachedPublicKey;
    }

    public static byte[] getPublicKeyBytes() {
        return getPublicKey().getEncoded();
    }

    public static PublicKey decodePublicKey(byte[] encoded) {
        try {
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Ed25519 public key", e);
        }
    }

    public static byte[] sign(byte[] nonce, String claimedHost) {
        ensureKeys();
        try {
            byte[] payload = buildPayload(nonce, claimedHost);
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(cachedPrivateKey);
            signer.update(payload);
            return signer.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign auth payload", e);
        }
    }

    /**
     * Verifies that a signature was produced by who it should be produced by
     */
    public static boolean verify(byte[] publicKeyBytes, byte[] nonce, String claimedHost, byte[] signature) {
        try {
            PublicKey pk = decodePublicKey(publicKeyBytes);
            byte[] payload = buildPayload(nonce, claimedHost);
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(pk);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (Exception e) {
            // Malformed key / signature bytes — treat as verification failure.
            Log.warn("Verification threw exception: @", e.getMessage());
            return false;
        }
    }

    /**
     * Builds the byte array that is signed/verified for sending to server
     */
    private static byte[] buildPayload(byte[] nonce, String claimedHost) {
        byte[] hostBytes = claimedHost.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] payload   = new byte[nonce.length + 1 + hostBytes.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        payload[nonce.length] = ':';
        System.arraycopy(hostBytes, 0, payload, nonce.length + 1, hostBytes.length);
        return payload;
    }

    /**
     * Ensures {@link #cachedPrivateKey} and {@link #cachedPublicKey} are populated.
     * Generates a fresh keypair if the settings are empty, then saves them.
     */
    private static synchronized void ensureKeys() {
        if (cachedPrivateKey != null && cachedPublicKey != null) return;

        String privB64 = Core.settings.getString("private-key");
        String pubB64  = Core.settings.getString("public-key");

        if (privB64 != null && !privB64.isBlank()
                && pubB64  != null && !pubB64.isBlank()) {
            // Load from settings.
            try {
                byte[] privBytes = java.util.Base64.getDecoder().decode(privB64.trim());
                byte[] pubBytes  = java.util.Base64.getDecoder().decode(pubB64.trim());

                KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
                cachedPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                cachedPublicKey  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

                Log.debug("Loaded Ed25519 keypair from settings.");
                return;
            } catch (Exception e) {
                Log.warn("Failed to load keypair from settings (@), regenerating.", e.getMessage());
                // Fall through to generation.
            }
        }

        // Generate a brand-new keypair and persist it.
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
            KeyPair kp = kpg.generateKeyPair();
            cachedPrivateKey = kp.getPrivate();
            cachedPublicKey  = kp.getPublic();

            String newPrivB64 = java.util.Base64.getEncoder().encodeToString(cachedPrivateKey.getEncoded());
            String newPubB64  = java.util.Base64.getEncoder().encodeToString(cachedPublicKey.getEncoded());

            Core.settings.put("private-key", newPrivB64);
            Core.settings.put("public-key",  newPubB64);

            Log.info("Generated new Ed25519 keypair and saved to settings.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Ed25519 keypair", e);
        }
    }

    /** Generates a cryptographically random 32-byte nonce. */
    public static byte[] generateNonce() {
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
}