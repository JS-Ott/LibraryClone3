package jso.libcat3.db;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO sql magic to connect to database and import items instead of overwriting files
// including duplicate checking (Title + SortBy)

public class DbSaveManager {
    private DbAdapter dbAdapter;

    public DbSaveManager(DbAdapter db) {
        dbAdapter = db;
    }

    public void importDb(String path) {
        try {


            dbAdapter.importNewLibrary(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportDb(File dbFile) {
        try {
            // destination file (on sdcard)
            File destination = getDestination("db");

            // streams and channels
            FileInputStream srcStream = new FileInputStream(dbFile);
            FileOutputStream destStream = new FileOutputStream(destination);
            FileChannel srcChannel = srcStream.getChannel();
            FileChannel destChannel = destStream.getChannel();

            destChannel.transferFrom(srcChannel, 0, srcChannel.size());

            srcStream.close();
            srcChannel.close();
            destStream.close();
            destChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SimpleDateFormat")
    private static File getDestination(String extension) {
        File sdcard = Environment.getExternalStorageDirectory();
        SimpleDateFormat s = new SimpleDateFormat("MM-dd_HH-mm");
        String format = s.format(new Date());
        String backupPath = "libcat3_" + format + "." + extension;
        return new File(sdcard, backupPath);
    }
}
