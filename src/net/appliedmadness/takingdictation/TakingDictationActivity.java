package net.appliedmadness.takingdictation;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class TakingDictationActivity extends Activity 
{
	//SoundFileManager soundFileManager;
	SoundRecorder soundRecorder;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        soundRecorder = new SoundRecorder(this);

        Button recordButton = (Button) this.findViewById(R.id.record_button);
        recordButton.setOnClickListener(new RecordButtonClickListener());

        Button stopButton = (Button) this.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new StopButtonClickListener());
        /*
        SoundRecorder record = new SoundRecorder();
        
        record.start();
        */
    }
    
    private class RecordButtonClickListener implements OnClickListener
    {

		@Override
		public void onClick(View v) {
			soundRecorder.start();
		}
    	
    }
    private class StopButtonClickListener implements OnClickListener
    {

		@Override
		public void onClick(View v) {
			soundRecorder.stop();
		}
    	
    }
    public void toastError(String error)
    {
    	Toast.makeText(this, error, Toast.LENGTH_LONG);
    }
}