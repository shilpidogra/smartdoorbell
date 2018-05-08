package com.example.shilpi.doorbellproject;

import android.app.Activity;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */


public class MainActivity extends Activity {

    /**
     *  Camera capture device wrapper
     */
    private DoorbellCamera mCamera;
    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;

    ButtonInputDriver mButtonInputDriver;

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    private HandlerThread mCloudThread;
    private Handler mCloudHandler;


    public final String TAG = "MainActivity";

    public final String BUTTON_GPIO_PIN = "BCM4";
    TextView textOutput;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            mDatabase = FirebaseDatabase.getInstance();
            mStorage = FirebaseStorage.getInstance();

            // Creates new handlers and associated threads for camera and networking operations.
            mCameraThread = new HandlerThread("CameraBackground");
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());

            mCloudThread = new HandlerThread("CloudThread");
            mCloudThread.start();
            mCloudHandler = new Handler(mCloudThread.getLooper());


            // Initialize the doorbell button driver
            mButtonInputDriver = new ButtonInputDriver(
                    BUTTON_GPIO_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();

            textOutput = findViewById(R.id.textOutput);


            mCamera = DoorbellCamera.getInstance();
            mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);



        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCamera.shutDown();

        mCameraThread.quitSafely();
        mCloudThread.quitSafely();
        try {
            if(mButtonInputDriver != null)
            {
                mButtonInputDriver.unregister();
            }
            mButtonInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

        /**
         * Listener for new camera images.
         */

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Get the raw image bytes
                    Image image = reader.acquireLatestImage();
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };


    /**
     * Upload image data to Firebase as a doorbell event.
     */

    private void onPictureTaken(final byte[] imageBytes) {

                if (imageBytes != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textOutput.setText("Image Clicked & Uploaded Successfully");
                        }
                    });

                    final DatabaseReference log = mDatabase.getReference("logs").push();
                    final StorageReference imageRef = mStorage.getReference().child(log.getKey());

                    // upload image to storage
                    UploadTask task = imageRef.putBytes(imageBytes);
                    task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // handle upload success
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            // mark image in the database
                            Log.i(TAG, "Image upload successful");
                            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                            log.child("image").setValue(downloadUrl.toString());
                            // process image annotations
                            annotateImage(log, imageBytes);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // handle upload failure
                            Log.w(TAG, "Unable to upload image to Firebase");
                            log.removeValue();

                        }

                    });

                }
            }

            /**
             * Process image contents with Cloud Vision.
             */

            private void annotateImage(final DatabaseReference ref, final byte[] imageBytes) {
                mCloudHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "sending image to cloud vision");
                        // annotate image by uploading to Cloud Vision API
                        try {
                            Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
                            Log.d(TAG, "cloud vision annotations:" + annotations);
                            if (annotations != null) {
                                ref.child("annotations").setValue(annotations);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Cloud Vison API error: ", e);
                        }
                    }
                });
            }
        }



