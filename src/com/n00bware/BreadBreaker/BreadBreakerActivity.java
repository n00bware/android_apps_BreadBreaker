package com.n00bware.BreadBreaker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BreadBreakerActivity extends Activity {

    private static final String TAG = "BreadBreaker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView status = (TextView)findViewById(R.id.tv_stat);
        status.setText("Hello and Welcome to BreadBreaker");

        Button mStepOne = (Button)findViewById(R.id.btn_step_1);
        mStepOne.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                unzipPayload();

                //do work
            }
        });

        Button mStepTwo = (Button)findViewById(R.id.btn_step_2);
        mStepTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //do work
            }
        });
    }

    final static String ZIP_FILTER = "assets";
    
    void unzipPayload() {
        String apkPath = getPackageCodePath();
        String mAppRoot = getFilesDir().toString();
        Log.d(TAG, String.format("apkPath { %s } && mAppRoot { %s }", apkPath, mAppRoot));
        try {
            File zipFile = new File(apkPath);
            long zipLastModified = zipFile.lastModified();
            ZipFile zip = new ZipFile(apkPath);
            Vector<ZipEntry> files = getAssets(zip);
            int zipFilterLength = ZIP_FILTER.length();
            
            Enumeration<?> entries = files.elements();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String path = entry.getName().substring(zipFilterLength);
                File outputFile = new File(mAppRoot, path);
                outputFile.getParentFile().mkdirs();

                if (outputFile.exists() && entry.getSize() == outputFile.length() && zipLastModified < outputFile.lastModified())
                    continue;
                FileOutputStream fos = new FileOutputStream(outputFile);
                copyStreams(zip.getInputStream(entry), fos);
                Runtime.getRuntime().exec("chmod 755 " + outputFile.getAbsolutePath());
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error: " + ioe.getMessage());
        } catch (NullPointerException ne) {
            Log.e(TAG, "NullPointerException: " + ne.getMessage());
        }
    }

    static final int BUFSIZE = 5192;

    void copyStreams(InputStream is, FileOutputStream fos) {
        BufferedOutputStream os = null;
        try {
            byte data[] = new byte[BUFSIZE];
            int count;
            os = new BufferedOutputStream(fos, BUFSIZE);
            while ((count = is.read(data, 0, BUFSIZE)) != -1) {
                os.write(data, 0, count);
            }
            os.flush();
        } catch (IOException e) {
            Log.e(TAG, "Exception while copying: " + e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e2) {
                Log.e(TAG, "Exception while closing the stream: " + e2);
            }
        }
    }

    public Vector<ZipEntry> getAssets(ZipFile zip) {
        Vector<ZipEntry> list = new Vector<ZipEntry>();
        Enumeration<?> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().startsWith(ZIP_FILTER)) {
                list.add(entry);
            }
        }
        return list;
    }
}
