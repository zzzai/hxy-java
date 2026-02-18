package com.zbkj.service.service.impl.payment;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.exception.CrmebException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信支付V3签名/验签/解密工具。
 */
public class WeChatPayV3Crypto {

    private static final Logger logger = LoggerFactory.getLogger(WeChatPayV3Crypto.class);

    private static final Map<String, PrivateKey> PRIVATE_KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, X509Certificate> CERT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PublicKey> PUBLIC_KEY_CACHE = new ConcurrentHashMap<>();

    private WeChatPayV3Crypto() {
    }

    public static String buildAuthorization(String mchId, String serialNo, String nonce, String timestamp, String signature) {
        return String.format(
                "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial_no=\"%s\",signature=\"%s\"",
                mchId, nonce, timestamp, serialNo, signature
        );
    }

    public static String signMessage(String message, String privateKeyPath) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new CrmebException("微信支付V3签名失败: " + e.getMessage());
        }
    }

    public static String signMiniProgramPay(String appId, String timestamp, String nonceStr, String packageValue, String privateKeyPath) {
        String message = appId + "\n" + timestamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        return signMessage(message, privateKeyPath);
    }

    public static boolean verifySignature(String message, String signatureBase64, String platformCertPath) {
        if (StrUtil.hasBlank(message, signatureBase64, platformCertPath)) {
            return false;
        }
        try {
            PublicKey publicKey = loadVerifyPublicKey(platformCertPath);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            logger.warn("微信支付V3验签失败, certPath={}, msg={}", platformCertPath, e.getMessage());
            return false;
        }
    }

    public static String decryptAesGcm(String apiV3Key, String associatedData, String nonce, String cipherText) {
        if (StrUtil.hasBlank(apiV3Key, nonce, cipherText)) {
            throw new CrmebException("微信支付V3解密参数不完整");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            if (StrUtil.isNotBlank(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CrmebException("微信支付V3解密失败: " + e.getMessage());
        }
    }

    public static X509Certificate loadX509Certificate(String certificatePath) {
        String cacheKey = "cert:" + StrUtil.trim(certificatePath);
        X509Certificate cached = CERT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream inputStream = openInputStream(certificatePath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            CERT_CACHE.put(cacheKey, certificate);
            return certificate;
        } catch (Exception e) {
            throw new CrmebException("读取微信支付平台证书失败: " + e.getMessage());
        }
    }

    private static PublicKey loadVerifyPublicKey(String keyPathOrCertPath) {
        try {
            return loadX509Certificate(keyPathOrCertPath).getPublicKey();
        } catch (Exception ignore) {
            // fall back to plain PEM public key
        }
        return loadPublicKey(keyPathOrCertPath);
    }

    private static PublicKey loadPublicKey(String publicKeyPath) {
        String cacheKey = "pub:" + StrUtil.trim(publicKeyPath);
        PublicKey cached = PUBLIC_KEY_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream inputStream = openInputStream(publicKeyPath)) {
            byte[] bytes = toBytes(inputStream);
            String pem = new String(bytes, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);
            PUBLIC_KEY_CACHE.put(cacheKey, publicKey);
            return publicKey;
        } catch (Exception e) {
            throw new CrmebException("读取微信支付验签公钥失败: " + e.getMessage());
        }
    }

    private static PrivateKey loadPrivateKey(String privateKeyPath) {
        String cacheKey = "pk:" + StrUtil.trim(privateKeyPath);
        PrivateKey cached = PRIVATE_KEY_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (InputStream inputStream = openInputStream(privateKeyPath)) {
            byte[] bytes = toBytes(inputStream);
            String pem = new String(bytes, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            PRIVATE_KEY_CACHE.put(cacheKey, privateKey);
            return privateKey;
        } catch (Exception e) {
            throw new CrmebException("读取商户私钥失败: " + e.getMessage());
        }
    }

    private static InputStream openInputStream(String path) throws Exception {
        if (StrUtil.isBlank(path)) {
            throw new CrmebException("证书路径为空");
        }
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            return new ClassPathResource(resourcePath).getInputStream();
        }
        return new FileInputStream(path);
    }

    private static byte[] toBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[4096];
        int n;
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        while ((n = inputStream.read(buffer)) > -1) {
            outputStream.write(buffer, 0, n);
        }
        return outputStream.toByteArray();
    }

    public static X509Certificate parseX509FromPemText(String pemText) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(pemText.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new CrmebException("解析平台证书失败: " + e.getMessage());
        }
    }
}
