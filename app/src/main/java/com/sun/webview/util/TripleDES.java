package com.sun.webview.util;

/**
 * @author 张胜飞
 * @Date 2022/7/30
 * @project XApi-SoftLicense
 * @Description {@link }
 */

import android.util.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TripleDES {

    private static final String Algorithm = "DESede";
    private static final String DESEDE_ECB_PADDING = "DESede/ECB/PKCS5Padding";
    private static final String DESDE_CBC_PADDING = "DESede/ECB/PKCS5Padding";
    private static final String originKey = "FreeBirdsEY9mNGonGgP4zQT";
    //private static final string IV = "12345678";
    //修改IV
    private static final byte[] keyIv = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};

    /**
     * 加密算法
     *
     * @param clearText
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String desEncript(String clearText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(DESEDE_ECB_PADDING); /*提供加密的方式：DES*/
        SecretKeySpec key = getKey(originKey);  /*对密钥进行操作，产生16个48位长的子密钥*/
        cipher.init(Cipher.ENCRYPT_MODE, key); /*初始化cipher，选定模式，这里为加密模式，并同时传入密钥*/
        byte[] doFinal = cipher.doFinal(clearText.getBytes());   /*开始加密操作*/
        String encode = null;    /*对加密后的数据按照Base64进行编码*/
        encode = Base64.encodeToString(doFinal, 1);
        return encode;
    }

    /**
     * 解密算法
     *
     * @param cipherText
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static String desDecript(String cipherText) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(DESEDE_ECB_PADDING);   /*初始化加密方式*/
        Key key = getKey(originKey);  /*获取密钥*/
        cipher.init(Cipher.DECRYPT_MODE, key);  /*初始化操作方式*/
        byte[] decode = new byte[0];  /*按照Base64解码*/
        decode = Base64.decode(cipherText, 1);
        byte[] doFinal = cipher.doFinal(decode);   /*执行解码操作*/
        return new String(doFinal);   /*转换成相应字符串并返回*/
    }

    /**
     * 获取密钥算法
     *
     * @param originKey
     * @return
     */
    private static SecretKeySpec getKey(String originKey) {
        byte[] buffer = new byte[24];
        byte[] originBytes = originKey.getBytes();
        /**
         * 防止输入的密钥长度超过192位
         */
        for (int i = 0; i < 24 && i < originBytes.length; i++) {
            buffer[i] = originBytes[i];  /*如果originBytes不足8,buffer剩余的补零*/
        }
        SecretKeySpec key = new SecretKeySpec(buffer, Algorithm); /*第一个参数是密钥字节数组，第二个参数是加密方式*/
        return key;  /*返回操作之后得到的密钥*/
    }

    /**
     * 向量加密算法
     *
     * @param clearText
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String desIVEncript(String clearText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(DESDE_CBC_PADDING); /*提供加密的方式：DES*/
        SecretKeySpec key = getKey(originKey);  /*对密钥进行操作，产生16个48位长的子密钥*/
        IvParameterSpec ivParameterSpec = new IvParameterSpec(keyIv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);  /*初始化操作方式*/ /*初始化cipher，选定模式，这里为加密模式，并同时传入密钥*/
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        byte[] doFinal = cipher.doFinal(clearText.getBytes());   /*开始加密操作*/
        String encode = null;    /*对加密后的数据按照Base64进行编码*/
        encode = Base64.encodeToString(doFinal, 1);
        return encode;
    }

    /**
     * 向量解密算法
     *
     * @param cipherText
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static String desIVDecript(String cipherText) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(DESDE_CBC_PADDING);   /*初始化加密方式*/
        Key key = getKey(originKey);  /*获取密钥*/
        IvParameterSpec ivParameterSpec = new IvParameterSpec(keyIv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);  /*初始化操作方式*/
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        byte[] decode = new byte[0];  /*按照Base64解码*/
        decode = Base64.decode(cipherText, 1);
        byte[] doFinal = cipher.doFinal(decode);   /*执行解码操作*/
        return new String(doFinal);   /*转换成相应字符串并返回*/
    }
}


