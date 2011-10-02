package net.appliedmadness.takingdictation;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;

public class SoundRecorder 
{
	private static final int COPY_BUFFER_SIZE = 1024;
	private static final int IN_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int OUT_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
	private static final int NUM_CHANNELS = 1;
	private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private static final int FORMAT_BITS = 16;
	private static final int SAMPLE_RATE = 44100;

	private static final String OUTPUT_EXTENSION = "wav";

	private static final int SUCCESS = 0;
	private static final int ERROR = -1;
	
	public static final String STORAGE_DIR_NAME = "Dictation";
	
	private int bufferSize;
	
	private AudioRecord mic;
	private File storageDir;
	private File outFile;
	
	private File tmpFile;
	
	private int errorState;
	private String errorMessage;
	
	private boolean stopped;
	
	private Thread recordingThread;
	private Thread playbackThread;
	private Runnable recordingJob;
	
	//private TakingDictationActivity context;
	
	private static SoundRecorder instance = null;
	
	private SoundRecorder()
	{
		
	}
	public static SoundRecorder getInstance()
	{
		if (instance == null)
		{
			instance = new SoundRecorder();
		}
		return instance;
	}
	private void init() throws IOException
	{
		//Check if drive storage is mounted.
		
		String storageState = Environment.getExternalStorageState();
		
		if (storageState.equals(Environment.MEDIA_MOUNTED))
		{
			File rootDir = Environment.getExternalStorageDirectory();
			storageDir = new File (rootDir.getAbsolutePath() + System.getProperty("file.separator") + STORAGE_DIR_NAME);
			
			boolean storageDirExists = storageDir.exists();
			
			if (!storageDirExists)
			{
				storageDirExists = storageDir.mkdir();
			}

			Date now = new Date();
			DateFormat fileDateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
			
			outFile = new File (storageDir.getAbsolutePath() + System.getProperty("file.separator") + fileDateFormat.format(now) + "." + OUTPUT_EXTENSION);
			tmpFile = File.createTempFile("TakingDictation", "wav", TakingDictationActivity.getInstance().getCacheDir());
			
			boolean outFileCanWrite = outFile.canWrite() || !outFile.exists();
			boolean tmpFileCanWrite = tmpFile.canWrite() || !tmpFile.exists();
			
			if (storageDirExists && outFileCanWrite && tmpFileCanWrite)
			{
				bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, IN_CHANNELS, FORMAT);
				mic = new AudioRecord(
						AudioSource.DEFAULT, 
						SAMPLE_RATE, 
						IN_CHANNELS, 
						FORMAT, 
						bufferSize);
				
				int recordingState = mic.getState();
				if (recordingState != AudioRecord.STATE_INITIALIZED)
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
			TakingDictationActivity.setReady(false);
			init();
			TakingDictationActivity.setReady(true);
		}
		catch (IOException e)
		{
			errorState = ERROR;
			errorMessage = e.getMessage();
		}
		if (errorState == SUCCESS)
		{
			TakingDictationActivity.setRecording(true);
			TakingDictationActivity.toastError("Recording to file: " + outFile.getAbsolutePath());
			recordingJob = new RecordingJob();
			recordingThread = new Thread(recordingJob);
			recordingThread.start();
		}
		else
		{
			TakingDictationActivity.toastError(errorMessage);
			TakingDictationActivity.setReady(true);
		}
	}
	public void stop()
	{

		TakingDictationActivity.setReady(false);
		try
		{
			if (mic != null && mic.getState() == AudioRecord.STATE_INITIALIZED)
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
					DataOutputStream outFileStream = new DataOutputStream(fos);
					
					// write the wav file per the wav file format
					outFileStream.writeBytes("RIFF");					// 00 - RIFF
					outFileStream.write(intToByteArray((int)(36 + tmpFile.length())), 0, 4);		// 04 - how big is the rest of this file?
					outFileStream.writeBytes("WAVE");					// 08 - WAVE
					outFileStream.writeBytes("fmt ");					// 12 - fmt 
					outFileStream.write(intToByteArray((int)16), 0, 4);	// 16 - size of this chunk
					outFileStream.write(shortToByteArray((short)1), 0, 2);		// 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
					outFileStream.write(shortToByteArray((short)NUM_CHANNELS), 0, 2);	// 22 - mono or stereo? 1 or 2?  (or 5 or ???)
					outFileStream.write(intToByteArray((int)SAMPLE_RATE), 0, 4);		// 24 - samples per second (numbers per second)
					outFileStream.write(intToByteArray((int)(SAMPLE_RATE * IN_CHANNELS * FORMAT_BITS / 8)), 0, 4);		// 28 - bytes per second
					outFileStream.write(shortToByteArray((short)(FORMAT_BITS / 8)), 0, 2);	// 32 - # of bytes in one sample, for all channels
					outFileStream.write(shortToByteArray((short)FORMAT_BITS), 0, 2);	// 34 - how many bits in a sample(number)?  usually 16 or 24
					outFileStream.writeBytes("data");					// 36 - data
					outFileStream.write(intToByteArray((int)tmpFile.length()), 0, 4);		// 40 - how big is this data chunk
					
					while ((bytesRead = fis.read(buffer)) > 0)
					{
						outFileStream.write(buffer, 0, bytesRead);
					}
					fis.close();
					fos.close();
				}
			}
		}
		catch (InterruptedException e)
		{
			
		}
		catch (Exception e)
		{
			
		}
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				SAMPLE_RATE, 
				OUT_CHANNELS,
				FORMAT,
				AudioTrack.getMinBufferSize(SAMPLE_RATE, OUT_CHANNELS, FORMAT),
				AudioTrack.MODE_STREAM);
		audioTrack.play();
		playbackThread = new Thread(new AudioStreamJob(tmpFile, audioTrack));
		playbackThread.start();
		try
		{
			playbackThread.join();
		}
		catch (InterruptedException e)
		{
			TakingDictationActivity.toastError("Playback interrupted.");
		}
		tmpFile.delete();
		TakingDictationActivity.setRecording(false);

		TakingDictationActivity.setReady(true);
		TakingDictationActivity.refresh();
	}
	
	public int getErrorState()
	{
		return errorState;
	}
	public String getErrorMessage()
	{
		return errorMessage;
	}
	
	private class AudioStreamJob implements Runnable
	{
		private File source;
		private AudioTrack destination;
		
		public AudioStreamJob(File mySource, AudioTrack myDestination)
		{
			source = mySource;
			destination = myDestination;
		}
		@Override
		public void run() {
			try
			{
				FileInputStream fis = new FileInputStream(source);
				byte[] buff = new byte[AudioTrack.getMinBufferSize(SAMPLE_RATE, OUT_CHANNELS, FORMAT)];
				
				int bytesRead;
				
				while ((bytesRead = fis.read(buff)) > 0)
				{
					destination.write(buff, 0, bytesRead);
				}
			}
			catch (FileNotFoundException e)
			{
				TakingDictationActivity.toastError("Lost tmp file somehow, could not play back.");
			}
			catch (IOException e)
			{
				TakingDictationActivity.toastError("Error reading from audio buffer.");
			}
		}
		
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
			mic = null;
		}
		
	}


// ===========================
// CONVERT BYTES TO JAVA TYPES
// ===========================

	// these two routines convert a byte array to a unsigned short
	public static int byteArrayToInt(byte[] b)
	{
		int start = 0;
		int low = b[start] & 0xff;
		int high = b[start+1] & 0xff;
		return (int)( high << 8 | low );
	}


	// these two routines convert a byte array to an unsigned integer
	public static long byteArrayToLong(byte[] b)
	{
		int start = 0;
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		
		for (i = start; i < (start + len); i++)
		{
			tmp[cnt] = b[i];
			cnt++;
		}
		
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 )
		{
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}


// ===========================
// CONVERT JAVA TYPES TO BYTES
// ===========================
	// returns a byte array of length 4
	private static byte[] intToByteArray(int i)
	{
		byte[] b = new byte[4];
		b[0] = (byte) (i & 0x00FF);
		b[1] = (byte) ((i >> 8) & 0x000000FF);
		b[2] = (byte) ((i >> 16) & 0x000000FF);
		b[3] = (byte) ((i >> 24) & 0x000000FF);
		return b;
	}

	// convert a short to a byte array
	public static byte[] shortToByteArray(short data)
	{
		return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
	}

}
