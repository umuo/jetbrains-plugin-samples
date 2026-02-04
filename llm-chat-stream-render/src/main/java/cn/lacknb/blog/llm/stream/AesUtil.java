package cn.lacknb.blog.llm.stream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

final class AesUtil {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private static final ThreadLocal<Cipher> CIPHER = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(TRANSFORMATION);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to init AES cipher", e);
        }
    });

    private AesUtil() {
    }

    static String decryptToString(byte[] data, byte[] key, byte[] iv) {
        byte[] plain = decrypt(data, key, iv);
        return plain == null ? null : new String(plain, StandardCharsets.UTF_8);
    }

    static byte[] decrypt(byte[] data, byte[] key, byte[] iv) {
        if (data == null || data.length == 0) {
            return null;
        }
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
            throw new IllegalArgumentException("Invalid AES key length");
        }
        if (iv == null || iv.length == 0) {
            throw new IllegalArgumentException("Invalid AES IV");
        }
        try {
            Cipher cipher = CIPHER.get();
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            return null;
        }
    }
}
