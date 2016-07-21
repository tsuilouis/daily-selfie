package com.tsui.louis.dailyselfie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

/**
 *
 * Requirement #3
 */
public class FullScreen extends Activity {

    private static final String TAG = "FullScreen";

    private ImageView mImageView;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        mImageView = (ImageView) findViewById(R.id.full_screen_image_view);

        Intent intent = getIntent();
        mCurrentPhotoPath = intent.getStringExtra(DailySelfie.EXTRA_IMAGE);

        // less memory-efficient
        // mImageView.setImageURI(Uri.parse(mCurrentPhotoPath));

        // this technique as described http://stackoverflow.com/a/24035591
        // runs setPic() after all of the View's operations
        mImageView.post(new Runnable() {
            @Override
            public void run() {
                setPic(mCurrentPhotoPath);
            }
        });
    }

    /*
     * Decode a scaled image
     * Use this approach to reduce memory consumed from displaying images
     */
    private void setPic(String mCurrentPhotoPath) {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        //int targetW = mImageView.getMeasuredWidth(); // statically defined
        //int targetH = mImageView.getMeasuredHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }
}
