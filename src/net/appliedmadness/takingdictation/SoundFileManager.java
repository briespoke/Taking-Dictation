package net.appliedmadness.takingdictation;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;

public class SoundFileManager {
	
	private static final String STORAGE_DIR_NAME = "Dictation";
	private MediaRecorder recorder; 
	private File storageDir;
	private File outFile;
	
	private File tmpFile;
	
	public SoundFileManager () throws IOException
	{
		//Check if drive storage is mounted.
		if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
		{

			File rootDir = Environment.getExternalStorageDirectory();
			storageDir = new File (rootDir.getAbsolutePath() + STORAGE_DIR_NAME);
			
			boolean storageDirExists = storageDir.exists();
			
			if (!storageDirExists)
			{
				storageDirExists = storageDir.mkdir();
			}
			if (!storageDirExists)
			{
				throw new IOException("Failed to create storage directory");
			}

		}
		else
		{
			throw new IOException("Android media (memory card) not available.");
		}

	}
	public String getNewFilePath() throws IOException
	{
		Date now = new Date();
		DateFormat fileDateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.wav");
		
		outFile = new File (storageDir.getAbsolutePath() + System.getProperty("file.separator") + fileDateFormat.format(now) + ".mp3");
		tmpFile = File.createTempFile("TakingDictation", "wav");
		
		if (outFile.canWrite() && tmpFile.canWrite())
		{
			return outFile.getAbsolutePath();
		}
		else
		{
			throw new IOException("Error accessing Android media (memory card).");				
		}
	}
	
	public void record() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(AudioSource.MIC);
		recorder.start();
	}
}
