package ninja.sosneaky.thomaslauer.panorama;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static ninja.sosneaky.thomaslauer.panorama.R.id.imageView;

public class SliceActivity extends AppCompatActivity {

    private static final String LOG = "SliceActivity";

    private Bitmap mFullBitmap;
    private ArrayList<Bitmap> mSegmentedBitmap;

    Thread workerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slice);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        Uri imageUri = null;

        if(action == null && type == null) {
            // called from inside app
            Log.d(LOG, "Called from inside app!");
            imageUri = Uri.parse(intent.getStringExtra("imagePath"));
        } else if(Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d(LOG, "Called from outside app!");
            imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        ImageView view = (ImageView) findViewById(imageView);
        view.setImageURI(imageUri);

        try {
            mFullBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void runSliceThread(View view) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();
        workerThread = new Thread(new PanoramaTask(this));
        workerThread.run();
    }

    private class PanoramaTask implements Runnable {

        private Activity activity;

        public PanoramaTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            int width = mFullBitmap.getWidth();
            int height = mFullBitmap.getHeight();

            mSegmentedBitmap = new ArrayList<>();

            makeToast("Starting Processing");


            if(width < height) {
                Log.e(LOG, "Image is not wide enough");

                makeToast("Image is not wide enough");

            } else {

                int numberOfSquares = width / height;
                //int extraWidth = (width - numberOfSquares * height);

                int segmentSize = height; // maybe change how this is done

                int currentSegment = 0;
                while(currentSegment < numberOfSquares) {

                    Log.d(LOG, "Segmenting part " + currentSegment + "/" + numberOfSquares);

                    int offset = (width % height) / 2;

                    Bitmap currentBitmapSegment = Bitmap.createBitmap(mFullBitmap, currentSegment * segmentSize + offset, 0,
                            segmentSize, segmentSize);

                    mSegmentedBitmap.add(currentBitmapSegment);

                    currentSegment++;
                }

                saveAllImages();

                makeToast("Finished Processing!");
            }
        }

        public void makeToast(final String text) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void saveAllImages() {
            for(int i = 0; i < mSegmentedBitmap.size(); i++) {
                storeImage(mSegmentedBitmap.get(mSegmentedBitmap.size() - i - 1), i);
            }
        }

        private void storeImage(Bitmap image, int index) {
            File pictureFile = getOutputMediaFile(index);
            if (pictureFile == null) {
                Log.d(LOG,
                        "Error creating media file, check storage permissions: ");// e.getMessage());
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                image.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(LOG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOG, "Error accessing file: " + e.getMessage());
            }
        }

        private File getOutputMediaFile(int indexNumber){
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
//        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
//                + "/Android/data/"
//                + getApplicationContext().getPackageName()
//                + "/Files");

            File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                    + "/panoramas");

            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    return null;
                }
            }
            // Create a media file name
            String timeStamp = new SimpleDateFormat("dd-MM-yyyy hh::mm::ss", Locale.US).format(new Date());
            File mediaFile;
            String mImageName="MI_" + timeStamp + "_" + Integer.toString(indexNumber) + ".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);

            Log.d(LOG, "Writing to " + mediaFile.getAbsolutePath());

            return mediaFile;
        }
    }
}
