package net.appliedmadness.takingdictation;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javazoom.jl.converter.Converter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;

public class SoundRecorder
{
	private static final int COPY_BUFFER_SIZE = 1024;
	private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int SAMPLE_RATE = 44100;

	private static final String OUTPUT_EXTENSION = "wav";

	private static final int SUCCESS = 0;
	private static final int ERROR = -1;
	
	private static final String STORAGE_DIR_NAME = "Dictation";
	
	private int bufferSize;
	
	private AudioRecord mic;
	private File storageDir;
	private File outFile;
	
	private File tmpFile;
	
	private int errorState;
	private String errorMessage;
	
	private boolean stopped;
	
	private Thread recordingThread;
	private Runnable recordingJob;
	
	private TakingDictationActivity context;
	
	public SoundRecorder(TakingDictationActivity myContext)
	{
		context = myContext;
	}
	private void init() throws IOException
	{
		//Check if drive storage is mounted.
		
		String storageState = Environment.getExternalStorageState();
		
		if (storageState.equals(Environment.MEDIA_MOUNTED))
		{
			File rootDir = Environment.getExternalStorageDirectory();
			storageDir = new File (rootDir.getAbsolutePath() + STORAGE_DIR_NAME);
			
			boolean storageDirExists = storageDir.exists();
			
			if (!storageDirExists)
			{
				storageDirExists = storageDir.mkdir();
			}

			Date now = new Date();
			DateFormat fileDateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
			
			outFile = new File (storageDir.getAbsolutePath() + System.getProperty("file.separator") + fileDateFormat.format(now) + OUTPUT_EXTENSION);
			tmpFile = File.createTempFile("TakingDictation", "wav");
			
			if (storageDirExists && outFile.canWrite() && tmpFile.canWrite())
			{
				bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, FORMAT);
				mic = new AudioRecord(
						AudioSource.MIC, 
						SAMPLE_RATE, 
						CHANNELS, 
						FORMAT, 
						bufferSize);
				if (mic.getState() != AudioRecord.STATE_INITIALIZED)
				{
					throw new IOException("Error initializing microphone.");				
				}
			}
			else
			{
				throw new IOException("Error accessing Android media (memory card).");				
			}
		}
		else
		{
			throw new IOException("Android media (memory card) not available.");
		}

	}

	public void start()
	{
		// I'm feeling optimistic.
		errorState = SUCCESS;
		stopped = false;
		try 
		{
			init();
		}
		catch (IOException e)
		{
			errorState = ERROR;
			errorMessage = e.getMessage();
		}
		if (errorState == SUCCESS)
		{
			context.toastError("Recording to file: " + outFile.getAbsolutePath());
			recordingJob = new RecordingJob();
			recordingThread = new Thread(recordingJob);
			recordingThread.start();
		}
		else
		{
			context.toastError(errorMessage);
		}
	}
	public void stop()
	{
		try
		{
			stopped = true;
			recordingThread.join();

			boolean renameSuccess = tmpFile.renameTo(outFile);
			
			if (! renameSuccess)
			{
				FileInputStream fis = new FileInputStream(tmpFile);
				FileOutputStream fos = new FileOutputStream(outFile);
				
				byte buffer[] = new byte[COPY_BUFFER_SIZE];
				int bytesRead;
				while ((bytesRead = fis.read(buffer)) > 0)
				{
					fos.write(buffer, 0, bytesRead);
				}
				fis.close();
				fos.close();
			}
		}
		catch (InterruptedException e)
		{
			
		}
		catch (Exception e)
		{
			
		}
	}
	
	public int getErrorState()
	{
		return errorState;
	}
	public String getErrorMessage()
	{
		return errorMessage;
	}
	private class RecordingJob implements Runnable
	{

		@Override
		public void run() {
			try
			{
				byte tmpBuffer[] = new byte[bufferSize];
				int bytesRead;
				String moo = tmpFile.getAbsolutePath();
				FileOutputStream fos = new FileOutputStream(tmpFile);
				
				mic.startRecording();
				while ((bytesRead = mic.read(tmpBuffer, 0, bufferSize)) >= AudioRecord.SUCCESS &&
						! stopped
						)
				{
					fos.write(tmpBuffer, 0, bytesRead);
				}
				
			}
			catch (IOException e)
			{
				errorState = ERROR;
				errorMessage = e.getMessage();
			}
			mic.stop();
			mic.release();
		}
		
	}
}
