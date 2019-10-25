/*
 * Copyright 2019 Louis Ohlow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package louisohlow.augre.com.augrearcore;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.core.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {

    private ArSceneView arView;
    private Session mSession;
    public String TAG = "Mainactivity: ";
    private boolean shouldConfigureSession = false;
    private VideoRecorder videoRecorder;

    private String imgNode = "head3.jpg";
    private boolean renderable_created = false;

    int REQUEST_CODE = 1;

    @Nullable
    private ModelRenderable videoRenderable;
    private MediaPlayer mediaPlayer;

    Button recordButton;

    private ExternalTexture texture;

    // Controls the height of the video in world space.
    private static final float VIDEO_HEIGHT_METERS = 0.85f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View
        arView = findViewById(R.id.arView);
        //RequestPermission
        requestPermissions();

        initVidRecorder();

        initSceneView();

        initButtons();

        // Create an ExternalTexture for displaying the contents of the video.
        texture = new ExternalTexture();

        // Create an Android MediaPlayer to capture the video on the external texture's surface.
        // videoX
        mediaPlayer = MediaPlayer.create(this, R.raw.head3);
        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(true);
    }

    private void initButtons() {
        recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(view -> {
            if (videoRecorder.onToggleRecord()) {
                Toast.makeText(this, "started recording", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "saved video in " + videoRecorder.getVideoPath().getAbsolutePath(), Toast.LENGTH_SHORT).show();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(videoRecorder.getVideoPath().getAbsolutePath());
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            }
        });
    }

    private void initSceneView() {
        arView.getScene().addOnUpdateListener(this);
    }

    private void setupSession() {
        if (mSession == null) {
            try {
                mSession = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                e.printStackTrace();
            } catch (UnavailableApkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableSdkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableDeviceNotCompatibleException e) {
                e.printStackTrace();
            }
            shouldConfigureSession = true;

        }
        if (shouldConfigureSession) {
            configSession();
            shouldConfigureSession = false;
            arView.setupSession(mSession);
        }

        try {
            mSession.resume();
            arView.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
            mSession = null;
        }
    }

    private void configSession() {
        Config config = new Config(mSession);
        if (!buildDatabase(config)) {
            Toast.makeText(this, TAG + "Error Database", Toast.LENGTH_SHORT).show();
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

        mSession.configure(config);

    }

    private boolean buildDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap bitmap = loadImage();
        if (bitmap == null) {
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(mSession);
        augmentedImageDatabase.addImage("tribal", bitmap, 0.2f);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadImage() {
        try {
            InputStream is = getAssets().open(imgNode);
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "I/O exception loading augmented image bitmap.", e);
        }
        return null;
    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        requestPermissions();

        Frame frame = arView.getArFrame();
        Collection<AugmentedImage> updateAugmentedImg = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage image : updateAugmentedImg) {
            if (image.getTrackingState() == TrackingState.TRACKING) {
                if (image.getName().equals("tribal")) {
                    if(!renderable_created){
                        renderable_created=true;

                        ARNode node = new ARNode(this, R.raw.chroma_key_video, texture);
                        node.setImage(image);

                        arView.getScene().addChild(node);

                        // Create a node to render the video and add it to the anchor.
                        // Set the scale of the node so that the aspect ratio of the video is correct.
                        float vidWidth = mediaPlayer.getVideoWidth();
                        float vidHeight = mediaPlayer.getVideoHeight();
                        node.setLocalScale(
                                new Vector3(
                                        VIDEO_HEIGHT_METERS *(vidWidth/vidHeight), VIDEO_HEIGHT_METERS, 1.0f));

                        // Start playing the video when the first node is placed.
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();

                            // Wait to set the renderable until the first frame of the  video becomes available.
                            // This prevents the renderable from briefly appearing as a black quad before the video
                            // plays.
                            texture
                                    .getSurfaceTexture()
                                    .setOnFrameAvailableListener(
                                            (SurfaceTexture surfaceTexture) -> {
                                                node.setRenderable(videoRenderable);
                                                texture.getSurfaceTexture().setOnFrameAvailableListener(null);
                                                Log.d(TAG, "activated texture");
                                            });
                        } else {
                            node.setRenderable(videoRenderable);
                            Log.d(TAG, "no texture");
                        }
                        Thread t1 = new Thread(new alphaThread());
                        t1.start();
                    }
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //RequestPermission
        requestPermissions();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mSession != null) {
            arView.pause();
            mSession.pause();
        }
    }

    public void initVidRecorder() {
        videoRecorder = new VideoRecorder();
        // Specify the AR scene view to be recorded.
        videoRecorder.setSceneView(arView);

        // Set video quality and recording orientation to match that of the device.
        int orientation = getResources().getConfiguration().orientation;
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);
    }

    private void requestPermissions() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED){
            setupSession();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
        }
        /*Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            setupSession();
                        } else {
                            Toast.makeText(MainActivity.this, TAG + "Permission denied", Toast.LENGTH_SHORT).show();

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
                */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        requestPermissions();
    }
}
