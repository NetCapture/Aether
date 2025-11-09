package cn.demo.appq.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 证书保存工具类
 * 支持将证书保存到Android设备的Downloads目录
 */
public class CertificateSaver {

    private static final String TAG = "CertificateSaver";
    private Context context;

    public CertificateSaver(Context context) {
        this.context = context;
    }

    /**
     * 保存结果类
     */
    public static class SaveResult {
        public boolean isSuccess;
        public String errorMessage;
        public String savedFilePath;

        public SaveResult(boolean success, String error, String path) {
            this.isSuccess = success;
            this.errorMessage = error;
            this.savedFilePath = path;
        }
    }

    /**
     * 保存证书到Downloads目录
     */
    public SaveResult saveCertificateToDownloads(byte[] certData, String baseName, String format) {
        try {
            // Android 10+ (API 29+) 使用MediaStore，不需要特殊权限
            // Android 9及以下需要WRITE_EXTERNAL_STORAGE权限
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    return new SaveResult(false, "需要WRITE_EXTERNAL_STORAGE权限", "");
                }
            }

            // 准备文件名
            String fileName = baseName + "." + format.toLowerCase();

            // 保存文件
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore
                return saveUsingMediaStore(certData, fileName);
            } else {
                // Android 9及以下使用传统方法
                return saveUsingTraditionalFile(certData, fileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "保存证书失败", e);
            return new SaveResult(false, e.getMessage(), "");
        }
    }

    /**
     * 使用MediaStore API保存（Android 10+）
     */
    private SaveResult saveUsingMediaStore(byte[] data, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/x-pem-file");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Aether/");

            Uri uri = context.getContentResolver().insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);

            if (uri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(data);
                        out.flush();
                    }
                }
                String savedPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/Aether/" + fileName;
                return new SaveResult(true, "保存成功", savedPath);
            } else {
                return new SaveResult(false, "无法创建文件", "");
            }
        } catch (Exception e) {
            return new SaveResult(false, "MediaStore保存失败: " + e.getMessage(), "");
        }
    }

    /**
     * 使用传统文件API保存（Android 9及以下）
     */
    private SaveResult saveUsingTraditionalFile(byte[] data, String fileName) {
        try {
            File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Aether");
            if (!downloadsDir.exists()) {
                if (!downloadsDir.mkdirs()) {
                    return new SaveResult(false, "无法创建目录", "");
                }
            }

            File file = new File(downloadsDir, fileName);
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                bos.write(data);
                bos.flush();
            }

            return new SaveResult(true, "保存成功", file.getAbsolutePath());
        } catch (IOException e) {
            return new SaveResult(false, "文件保存失败: " + e.getMessage(), "");
        }
    }
}
