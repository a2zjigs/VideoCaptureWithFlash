package com.exercise.AndroidVideoCapture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.Toast;

public class AndroidVideoCapture extends Activity {

	private Camera myCamera;
	private MyCameraSurfaceView myCameraSurfaceView;
	private MediaRecorder mediaRecorder;

	Button myButton;
	RadioButton flashOff, flashTorch;
	SurfaceHolder surfaceHolder;
	boolean recording;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		recording = false;

		setContentView(R.layout.main);

		// Get Camera for preview
		myCamera = getCameraInstance();
		if (myCamera == null) {
			Toast.makeText(AndroidVideoCapture.this, "Fail to get Camera",Toast.LENGTH_LONG).show();
		}

		myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
		FrameLayout myCameraPreview = (FrameLayout) findViewById(R.id.videoview);
		myCameraPreview.addView(myCameraSurfaceView);

		myButton = (Button) findViewById(R.id.mybutton);
		myButton.setOnClickListener(myButtonOnClickListener);

		flashOff = (RadioButton) findViewById(R.id.flashoff);
		flashTorch = (RadioButton) findViewById(R.id.flashtorch);
	}

	Button.OnTouchListener flashButtonOnTouchListener = new Button.OnTouchListener() {

		@Override
		public boolean onTouch(View arg0, MotionEvent arg1) {
			// TODO Auto-generated method stub

			if (myCamera != null) {
				Parameters parameters = myCamera.getParameters();

				switch (arg1.getAction()) {
				case MotionEvent.ACTION_DOWN:
					parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
					myCamera.setParameters(parameters);
					break;
				case MotionEvent.ACTION_UP:
					parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
					myCamera.setParameters(parameters);
					break;
				}
				;
			}

			return true;
		}
	};

	Button.OnClickListener flashModeButtonOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub

		}
	};

	Button.OnClickListener myButtonOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (recording) {
				// stop recording and release camera
				mediaRecorder.stop(); // stop the recording
				releaseMediaRecorder(); // release the MediaRecorder object

				myCamera.lock(); 
				// take camera access back from
				recording = false;
				// MediaRecorder
				// inform the user that recording has stopped
				// Exit after saved
				//finish();
			} else {

				// Release Camera before MediaRecorder start
				releaseCamera();

				if (!prepareMediaRecorder()) {
					Toast.makeText(AndroidVideoCapture.this,
							"Fail in prepareMediaRecorder()!\n - Ended -",
							Toast.LENGTH_LONG).show();
					finish();
				}

				mediaRecorder.start();
				recording = true;
				myButton.setText("STOP");
			}
		}
	};

	private Camera getCameraInstance() {
		// TODO Auto-generated method stub
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
			c.setDisplayOrientation(90);
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private String getFlashModeSetting() {
		if (flashTorch.isChecked()) {
			return Parameters.FLASH_MODE_TORCH;
		} else {
			return Parameters.FLASH_MODE_OFF;
		}
	}

	private boolean prepareMediaRecorder() {
		myCamera = getCameraInstance();

		Parameters parameters = myCamera.getParameters();
		parameters.setFlashMode(getFlashModeSetting());
		myCamera.setParameters(parameters);

		mediaRecorder = new MediaRecorder();

		myCamera.unlock();
		mediaRecorder.setCamera(myCamera);

		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

		mediaRecorder.setOutputFile(getOutputMediaFile("movie"));
		mediaRecorder.setMaxDuration(6000000); // Set max duration 60 sec.
		mediaRecorder.setMaxFileSize(50000000); // Set max file size 5M

		mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());

		mediaRecorder.setOrientationHint(90);

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it
								// first
		releaseCamera(); // release the camera immediately on pause event
	}

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			myCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (myCamera != null) {
			myCamera.release();
			// release the camera for other applications
			myCamera = null;
		}
	}

	public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

		private SurfaceHolder mHolder;
		private Camera mCamera;

		public MyCameraSurfaceView(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,int weight, int height) {
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// make any resize, rotate or reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}
	}

	private static String getOutputMediaFile(String sufix) {

		String mediaFile;
		File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "/YappBack");
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("VideoLogger", "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		if (!sufix.equals("movie")) {
			mediaFile = mediaStorageDir.getPath() + File.separator + "output_"+ timeStamp + "_" + sufix + ".txt";
		} else {
			mediaFile = mediaStorageDir.getPath() + File.separator + "output_"+ timeStamp + ".mp4";

		}
		//mediaFile = Environment.getExternalStorageDirectory().getPath()+ "/default.mp4";
		return mediaFile;
	}
}