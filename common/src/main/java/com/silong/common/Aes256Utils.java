package com.silong.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.annotation.Nonnull;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotBlank;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AES256加解密工具
 *
 * @author louis sin
 * @version 1.0
 * @since 20180506
 */
public final class Aes256Utils {

  private static final Logger LOGGER = LoggerFactory.getLogger(Aes256Utils.class);
  private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
  private static final String PBKDF2_WITH_HMAC_SHA1 = "PBKDF2WithHmacSHA1";
  private static final String AES = "AES";
  private static final char[] DEFAULT_PASSWORD = "20180506shenzhenfutian".toCharArray();
  private static final int PWD_ITERATION = 1000;
  private static final int KEY_SIZE = 256;
  private static int SALT_LENGTH = KEY_SIZE / 8;

  /**
   * 私有构造方法
   */
  private Aes256Utils() {
  }

//	public static void main(String[] args) throws Exception {
//		System.out.println(encrypt("Shi00@123"));
//	}

  /**
   * 加密指定字符串
   *
   * @param plainText 待加密字符串
   * @return 加密后字符串
   */
  @Nonnull
  public static String encrypt(@NotBlank String plainText) {

    if (isBlank(plainText)) {
      throw new IllegalArgumentException("plainText must not be null or blank.");
    }

    // get salt
    byte[] saltBytes = generateSalt();

    try {
      // Derive the key
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA1);
      PBEKeySpec spec = new PBEKeySpec(DEFAULT_PASSWORD, saltBytes, PWD_ITERATION, KEY_SIZE);

      SecretKeySpec secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);

      // encrypt the message
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
      cipher.init(Cipher.ENCRYPT_MODE, secret);
      AlgorithmParameters params = cipher.getParameters();
      byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
      byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes(UTF_8));

      // Base64 for Java
      return String.format("%s]%s]%s", Base64.encodeBase64String(saltBytes),
          Base64.encodeBase64String(ivBytes),
          Base64.encodeBase64String(encryptedTextBytes));
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | InvalidParameterSpecException | IllegalBlockSizeException | BadPaddingException e) {
      LOGGER.error("Failed to encrypt *.", e);
      return EMPTY;
    }
  }

  /**
   * 解密指定字符串
   *
   * @param encryptedText 待解密字符串
   * @return 解密后字符串
   */
  @Nonnull
  public static String decrypt(@NotBlank String encryptedText) {
    if (isBlank(encryptedText)) {
      throw new IllegalArgumentException("encryptedText must not be null or blank.");
    }

    String[] fields = encryptedText.split("]");
    byte[] saltBytes = Base64.decodeBase64(fields[0]);
    byte[] ivBytes = Base64.decodeBase64(fields[1]);
    byte[] encryptedTextBytes = Base64.decodeBase64(fields[2]);

    try {
      // Derive the key
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA1);
      PBEKeySpec spec = new PBEKeySpec(DEFAULT_PASSWORD, saltBytes, PWD_ITERATION, KEY_SIZE);

      SecretKey secretKey = factory.generateSecret(spec);
      SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), AES);

      // Decrypt the message
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

      return new String(cipher.doFinal(encryptedTextBytes), UTF_8);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalBlockSizeException | BadPaddingException
        | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
      LOGGER.error("Failed to decrypt *.", e);
      return EMPTY;
    }
  }

  private static byte[] generateSalt() {
    SecureRandom random = new SecureRandom();
    byte bytes[] = new byte[SALT_LENGTH];
    random.nextBytes(bytes);
    return bytes;
  }

}