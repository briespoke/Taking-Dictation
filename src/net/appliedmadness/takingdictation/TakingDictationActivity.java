package net.appliedmadness.takingdictation;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * The main and only activity.
 * 
 * @author wavenger
 *
 */
public class TakingDictationActivity extends Activity 
{
	SoundRecorder soundRecorder;
	
	private Button recordButton;
	private Button stopButton;
	
	private FileViewer viewer;
	
	private static TakingDictationActivity instance;

	private static final String IS_READY = "ready";
	private static final String IS_RECORDING = "recording";
	
	private boolean isReady, isRecording;
	
	public static TakingDictationActivity getInstance()
	{
		return instance;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        instance = this;
        
        setContentView(R.layout.main);
        
        soundRecorder = SoundRecorder.getInstance();
        
        recordButton = (Button) this.findViewById(R.id.record_button);
        recordButton.setOnClickListener(new RecordButtonClickListener());

        stopButton = (Button) this.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new StopButtonClickListener());
        
        if (savedInstanceState != null)
        {
	        isReady = savedInstanceState.getBoolean(IS_READY, true);
	        isRecording = savedInstanceState.getBoolean(IS_RECORDING, false);
        }
        else
        {
        	isReady = true;
        	isRecording = false;
        }
        setButtonStates();
        
        viewer = (FileViewer) this.findViewById(R.id.file_viewer);
		
        TakingDictationActivity.refresh();
    }
    
    
    /**
     * Called to ensure that button states properly reflect the internal state of the activity.
     */
    private void setButtonStates() {
		recordButton.setEnabled(isReady && !isRecording);
		stopButton.setEnabled(isReady && isRecording);
	}

	/**
	 * This allows us to preserve the application's state when it is closed or the view is reoriented.
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
    {
    	savedInstanceState.putBoolean(IS_READY, isReady);
    	savedInstanceState.putBoolean(IS_RECORDING, isRecording);
    }
    /**
     * Start recording!
     * @author wavenger
     *
     */
    private class RecordButtonClickListener implements OnClickListener
    {

		@Override
		public void onClick(View v) {
			soundRecorder.start();
		}
    	
    }
    
    /**
     * Stop recording!
     * @author wavenger
     *
     */
    private class StopButtonClickListener implements OnClickListener
    {

		@Override
		public void onClick(View v) {
			soundRecorder.stop();
		}
    	
    }
    /**
     * Presents the user with an error (i.e., not an internal exception or fault - defined behavior).
     * @param error The error text.
     */
    public static void toastError(String error)
    {
    	if (instance != null)
    	{
    		Toast.makeText(instance, error, Toast.LENGTH_LONG).show();
    	}
    }
    
    /**
     * Indicate that the application is set up and ready to accept input.
     * @param readyFlag Whether the app is ready or not.
     */
    public static void setReady(boolean readyFlag)
    {
    	TakingDictationActivity instance = TakingDictationActivity.getInstance();
    	
    	instance.isReady = readyFlag;
    	
    	instance.setButtonStates();
    }
    /**
     * Indicates that the app is recording.
     * @param recordingFlag Whether the app is recording or not.
     */
    public static void setRecording(boolean recordingFlag)
    {
    	TakingDictationActivity instance = TakingDictationActivity.getInstance();
    	
    	instance.isRecording = recordingFlag;
    	
    	instance.setButtonStates();
    }
    
    /**
     * Refresh, making newly recorded files visible to the user.
     */
    public static void refresh()
    {
    	TakingDictationActivity instance = TakingDictationActivity.getInstance();

        String storageState = Environment.getExternalStorageState();
		
		if (storageState.equals(Environment.MEDIA_MOUNTED))
		{
			File rootDir = Environment.getExternalStorageDirectory();
			File storageDir = new File (rootDir.getAbsolutePath() + System.getProperty("file.separator") + SoundRecorder.STORAGE_DIR_NAME);
	        instance.viewer.init(storageDir);
		}
		else
		{
			TakingDictationActivity.toastError("Media not mounted, cannot display files");
		}
    }
}