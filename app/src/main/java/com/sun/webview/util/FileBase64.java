package com.sun.webview.util;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileBase64 {

    /**
     * encodeBase64File:(将文件转成base64 字符串).
     * @param path 文件路径
     * @return
     * @throws Exception
     */

    public static String encodeBase64File( String path ) throws Exception {

        File file = new File(path);
        FileInputStream inputFile = new FileInputStream(file);
        byte[] buffer = new byte[(int)file.length()];
        inputFile.read(buffer);
        inputFile.close();
        return Base64.encodeToString(buffer,Base64.DEFAULT);

    }

    /**
     * decoderBase64File:(将base64字符解码保存文件).

     * @param base64Code 编码后的字串
     * @param savePath  文件保存路径
     * @throws Exception

     */
    public  static void decoderBase64File(String base64Code,String savePath) throws Exception {

        byte[] buffer = Base64.decode(base64Code, Base64.DEFAULT);
        FileOutputStream out = new FileOutputStream(savePath);
        out.write(buffer);
        out.close();

    }

}