package com.yit.deploy.core.support;

import com.yit.deploy.core.info.DeployTableResponse;
import com.yit.deploy.core.utils.Utils;
import com.yit.deploy.core.variables.variable.ContextualVariable;
import hudson.FilePath;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public interface AlgorithmSupport {

    default String md5Digest(String text) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        byte[] bytes = digest.digest(text.getBytes(Utils.DefaultCharset));
        return new String(Hex.encodeHex(bytes));
    }

    default String md5Digest(FilePath filePath) throws IOException, InterruptedException {
        if (filePath.isDirectory()) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }

            for (FilePath path : filePath.list("**/*")) {
                if (path.isDirectory()) continue;
                digest.update(path.readToString().getBytes(Utils.DefaultCharset));
            }
            return new String(Hex.encodeHex(digest.digest()));
        }

        return md5Digest(filePath.readToString());
    }

    default Map parseJson(String json) {
        return DeployTableResponse.GSON.fromJson(json, Map.class);
    }

    default String toJson(Object value) {
        if (value instanceof ContextualVariable) {
            value = ((ContextualVariable) value).concrete();
        }
        return DeployTableResponse.GSON.toJson(value);
    }

    default String toBase64(String text) {
        return toBase64(text.getBytes(Charsets.UTF_8));
    }

    default String toBase64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), Charsets.UTF_8);
    }

    default byte[] fromBase64(String text) {
        return Base64.getDecoder().decode(text.getBytes(Charsets.UTF_8));
    }
}
