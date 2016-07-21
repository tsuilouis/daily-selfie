package com.tsui.louis.dailyselfie;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/*
 * Intro guide to Camera usage via intents:
 * https://developer.android.com/training/camera/photobasics.html
 *
 */
public class DailySelfie extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    static final int REQUEST_TAKE_PHOTO = 1;
    private static final String TAG = "DailySelfie";
    public static final String EXTRA_IMAGE = "com.tsui.louis.dailyselfie.IMAGE";
    private static final int URL_LOADER = 0;

    private Uri photoURI;
    private String mCurrentPhotoPath;
    private ListView mListView;
    private SimpleCursorAdapter mAdapter;

    private AlarmManager mAlarmManager;
    private Intent mNotificationReceiverIntent;
    private PendingIntent mNotificationReceiverPendingIntent;
    private static final long INITIAL_ALARM_DELAY = 2 * 60 * 1000L;
    private static final long REPEAT_ALARM_DELAY = 2 * 60 * 1000L;

    static final Uri mDataUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    // columns that will be retrieved for each row
    static final String[] PROJECTION = new String[] {
            //MediaStore.Images.ImageColumns._ID,
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA
    };

    // currently no select criteria
    // TODO: show only images taken by this app from the external public directory
    //static final String SELECTION = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_selfie);

        Toolbar toolbar = (Toolbar) findViewById(R.id.daily_selfie_toolbar);
        setSupportActionBar(toolbar);

        mListView = (ListView) findViewById(R.id.photos_list_view);

        // for the cursor adapter, specify which columns go into which views
        // the DATA specifies a file path which is eventually used by the
        // SimpleCursorAdapter's bindView() and setViewImage() as an image Uri
        // and finally sets the image to the ImageView
        String[] mFromColumns = {MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.TITLE};
        int[] mToFields = {R.id.item_image_view, R.id.item_text_view};

        mAdapter = new SimpleCursorAdapter(
                this,                   // current Context
                R.layout.list_item,     // Layout for a single row
                null,                   // no Cursor yet
                mFromColumns,           // Cursor columns to use
                mToFields,              // Layout fields to use
                0                       // no flags
        );

        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent fullImageIntent = new Intent(DailySelfie.this, FullScreen.class);
                //Toast.makeText(DailySelfie.this, "Clicked id:"+id, Toast.LENGTH_LONG).show();

                ImageView imageView = (ImageView) view.findViewById(R.id.item_image_view);
                /*Toast.makeText(DailySelfie.this,
                        "Clicked tag: "+imageView.getTag(),
                        Toast.LENGTH_LONG).show();*/

                // the index passed to the cursor corresponds to column indices in PROJECTION
                String imageFilePath = mAdapter.getCursor().getString(
                        Arrays.asList(PROJECTION).indexOf(MediaStore.MediaColumns.DATA));
                imageView.setTag(imageFilePath);
                Toast.makeText(DailySelfie.this, imageFilePath, Toast.LENGTH_LONG).show();
                fullImageIntent.putExtra(EXTRA_IMAGE, imageFilePath);
                startActivity(fullImageIntent);
            }
        });

        // getLoaderManager().initLoader(URL_LOADER, null, this);
        getSupportLoaderManager().initLoader(URL_LOADER, null, this);

        // create the repeating alarm that will relaunch this activity
        setupAlarm();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.activity_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_camera:
                // access the front camera to take a picture
                dispatchTakePictureIntent();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     *
     *
     *
     * @param loaderID
     * @param bundle
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        switch(loaderID) {
            case URL_LOADER:
                return new CursorLoader(
                    this,
                    mDataUrl,
                    PROJECTION,
                    null,
                    null,
                    null
                );
            default: // an invalid id was passed
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // move query results into adapter causing the ListView fronting this adapter to update
        //mAdapter.changeCursor(cursor);
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // clear out adapter reference to the Cursor; prevents memory leaks
        //mAdapter.changeCursor(null);
        mAdapter.swapCursor(null);
    }

    /*
     *
     * Take a photo with the camera app
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Intent takePictureIntent = new Intent(Intent.ACTION_PICK,
        //        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // Galaxy Nexus API 18 emulator's camera crashes for some reason
                // using Taking Photos Simply training approach
                // Might be related to FileProvider?
                // Modify based on http://stackoverflow.com/a/13069981

//                Uri photoURI = FileProvider.getUriForFile(this,
//                        "com.tsui.louis.fileprovider",
//                        photoFile);

                // Might be an issue with Samsung phones (actual or emulated)
                // as described here http://stackoverflow.com/a/23802526
                photoURI = Uri.fromFile(photoFile);

                // don't need to specify an ACTION for the Intent since it is explicit
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /*
     * display thumbnail
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // Test thumbnail
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    // check thumbnail from the Intent data
                    // mImageView.setImageBitmap(imageBitmap);
                } else { // Samsung phones
                    Toast.makeText(this, "Image saved to:\n" +
                            photoURI.toString(), Toast.LENGTH_LONG).show();
                }
                galleryAddPic();
                //mAdapter.notifyDataSetChanged();
            }

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = getAlbumStorageDir("Pictures/Selfies");

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File getAlbumStorageDir(String albumName) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);

            if (!file.exists()) {
                if (!file.mkdirs()) {
                    Log.e(TAG, "Directory not created");
                    return null;
                }
            }

            return file;
        }
        return null;
    }


    /*
     * As per Camera training guide
     * Also see http://stackoverflow.com/a/15837638
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file URL as it may be null upon screen orientation changes
        outState.putParcelable("photo_uri", photoURI);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get file URL
        photoURI = savedInstanceState.getParcelable("photo_uri");
    }

    /*
     * Method that sets up the alarm to go off after two minutes and then
     * every two minutes after that
     */
    private void setupAlarm() {
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNotificationReceiverIntent = new Intent(DailySelfie.this,
                AlarmNotificationReceiver.class);
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(DailySelfie.this,
                0, mNotificationReceiverIntent, 0);

        // the intervalMillis should be AlarmManager.INTERVAL_DAY for actual use purposes
        // as of API 19, all repeating alarms are inexact
        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INITIAL_ALARM_DELAY,
                REPEAT_ALARM_DELAY,
                mNotificationReceiverPendingIntent);
    }
}
