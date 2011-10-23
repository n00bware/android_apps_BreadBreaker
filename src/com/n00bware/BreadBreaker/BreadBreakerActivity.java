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

        final TextView status = (TextView)findViewById(R.id.tv_stat);
        status.setText("Hello and Welcome to BreadBreaker");

        Button mStepOne = (Button)findViewById(R.id.btn_step_1);
        mStepOne.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("Starting step one");
                StringBuilder step_one = new StringBuilder();
                Bin.runCmd("if [ -e /data/local/12m.bak ]; then rm /data/local/12m.bak; fi");
                step_one.append("mv /data/local/12m /data/local/12m.bak ; ");
                step_one.append("ln -s /data /data/local/12m ; ");
                Bin.runCmd(step_one.toString());
                status.setText("We are done with step 1 ...please reboot");
            }
        });

        Button mStepTwo = (Button)findViewById(R.id.btn_step_2);
        mStepTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("Starting step two");
                StringBuilder step_two = new StringBuilder();
                step_two.append("rm /data/local/12m ; ");
                step_two.append("mv /data/local/12m.bak /data/local/12m ; ");
                Bin.runCmd(step_two.toString());
                Bin.runCmd("if [ -e /data/local.prop.bak ]; then rm /data/local.prop.bak; fi");
                Bin.runCmd("mv /data/local.prop /data/local.prop.bak");
                Bin.runCmd("echo \"ro.sys.atvc_allow_netmon_usb=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_netmon_ih=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_res_core=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_res_panic=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_all_adb=1\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_all_core=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_efem=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_bp_log=0\" > /data/local.prop");
                Bin.runCmd("echo \"ro.sys.atvc_allow_ap_mot_log=0\" > /data/local.prop");
                Bin.runCmd("echo \"rro.sys.atvc_allow_gki_log=0\" > /data/local.prop");
                status.setText("Step two complete reboot now");

            }
        });
        Button mStepThree = (Button)findViewById(R.id.btn_step_3);
        mStepThree.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                unzipPayload();

                String filesDir = getFilesDir().getAbsolutePath();
                String busybox = filesDir + "/payload/busybox";
                String su = filesDir + "/payload/su";
                String su_apk = filesDir + "/payload/Superuser.apk";


                StringBuilder step_three = new StringBuilder();
                status.setText("attempting to install busybox and su binary");
                step_three.append("cp -rf " + busybox + " /system/xbin/busybox ; ");
                step_three.append("cp -rf " + su + " /system/xbin/su ; ");
                Bin.runCmd(step_three.toString());
                Bin.runCmd("install " + su_apk);
                Bin.runCmd("chmod 4755 /system/xbin/su");
                Bin.runCmd("chmod 755 /system/xbin/busybox");
                Bin.runCmd("/system/xbin/busybox --install -s /system/xbin/");
                Bin.runCmd("ln -s /system/xbin/su /system/bin/su");
                Bin.runCmd("chown system.system /data");
                status.setText("...and done your phone should be rooted now");
                
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
