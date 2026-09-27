package com.badminton.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds the RSA key pair used to encrypt credentials in transit.
 * The private key (keys/private_key.pem) never leaves the server;
 * the public key (keys/public_key.pem) is served to the frontend.
 */
@Slf4j
@Component
public class RsaKeyService {

	private static final String PRIVATE_KEY_PATH = "keys/private_key.pem";
	private static final String PUBLIC_KEY_PATH = "keys/public_key.pem";

	private PrivateKey privateKey;
	private String publicKeyPem;

	@PostConstruct
	void init() throws Exception {
		String privatePem = readPem(PRIVATE_KEY_PATH);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pemToDer(privatePem)));
		this.publicKeyPem = readPem(PUBLIC_KEY_PATH);
		log.info("RSA key pair loaded for credential encryption");
	}

	public String getPublicKeyPem() {
		return publicKeyPem;
	}

	/**
	 * Decrypts a Base64-encoded RSA-OAEP (SHA-256 / MGF1-SHA-256) ciphertext
	 * produced by the frontend's Web Crypto API.
	 */
	public String decrypt(String base64CipherText) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
			OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
					PSource.PSpecified.DEFAULT);
			cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec);
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64CipherText));
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to decrypt credentials", e);
		}
	}

	private String readPem(String path) throws Exception {
		ClassPathResource resource = new ClassPathResource(path);
		return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
	}

	private byte[] pemToDer(String pem) {
		String base64 = pem.replaceAll("-----BEGIN [A-Z ]+-----", "").replaceAll("-----END [A-Z ]+-----", "")
				.replaceAll("\\s", "");
		return Base64.getDecoder().decode(base64);
	}
}
