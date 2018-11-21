package jp.co.cyberagent.stf.monitor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.RequiresApi;
import android.util.Log;
import jp.co.cyberagent.stf.io.MessageWritable;
import jp.co.cyberagent.stf.proto.Wire;

import java.io.File;
import java.text.DecimalFormat;

import static java.util.jar.Pack200.Packer.ERROR;

public class StorageMonitor extends AbstractMonitor {
    private static final String TAG  = "STFStorageMonitor";

    private StorageState state = null;


    public StorageMonitor(Context context, MessageWritable writer) {
        super(context, writer);
    }

    @Override
    public void run() {
        Log.i(TAG, "Storage Monitor starting");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onReceive(Context context, Intent intent) {
                state = new StorageState(intent);
                report(writer, state);
            }
        };

        context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        try {
            synchronized (this) {
                while (!isInterrupted()) {
                    wait();
                }
            }
        }
        catch (InterruptedException e) {
            // Okay
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Log.i(TAG, "Monitor stopping");

            context.unregisterReceiver(receiver);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void peek(MessageWritable writer) {
        if (state != null) {
            report(writer, state);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void report(MessageWritable writer, StorageState state) {
        Log.i(TAG, String.format("Storage internal is %s, external is %s, system is %s",
                state.internal,
                state.external,
                state.system
        ));

        writer.write(Wire.Envelope.newBuilder()
            .setType(Wire.MessageType.EVENT_STORAGE)
            .setMessage(Wire.StorageEvent.newBuilder()
                .setExternal(state.external)
                .setInternal(state.internal)
                .setSystem(state.system)
                .build()
                .toByteString())
            .build());
    }



    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
            android.os.Environment.MEDIA_MOUNTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        Log.e(TAG, String.format("getInternalPath :%s", path));
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        Log.e(TAG, String.format("getInternal :%s", availableBlocks * blockSize));
        return formatSize(availableBlocks * blockSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return formatSize(totalBlocks * blockSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            Log.e(TAG, String.format("getExternalPath :%s", path));
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            Log.e(TAG, String.format("getExternal :%s", availableBlocks * blockSize));
            return formatSize(availableBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            return formatSize(totalBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getAvailableSystemySize() {
        if (externalMemoryAvailable()) {
//            File path = new File("/sdcard");
            File path = Environment.getRootDirectory();
            Log.e(TAG, String.format("getrootPath :%s", path));
            StatFs stat = new StatFs(path.getPath());
            float blockSize = stat.getBlockSizeLong();
            float availableBlocks = stat.getAvailableBlocksLong();
            float total = availableBlocks * blockSize;
            return formatSize(total);
        } else {
            return ERROR;
        }
    }


    private static String formatSize(float size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
                if (size >= 1024) {
                    suffix = "GB";
                    size /= 1024;
                }
            }
        }

        String new_size = new DecimalFormat("##.##").format(size);

//        StringBuilder resultBuffer = new StringBuilder(Float.toString(size));
//
//        int commaOffset = resultBuffer.length() - 3;
//        while (commaOffset > 0) {
//            resultBuffer.insert(commaOffset, ',');
//            commaOffset -= 3;
//        }
//
        if (suffix != null) {
            new_size = new_size + suffix;
        }
        return new_size;
//        return Float.toString(size);
    }


    private static class StorageState {
        private String internal;
        private String external;
        private String system;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public StorageState(Intent intent) {
            internal = getAvailableInternalMemorySize();
            external = getAvailableExternalMemorySize();
            system = getAvailableSystemySize();

        }
    }
}
