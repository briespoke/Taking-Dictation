package net.appliedmadness.takingdictation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Views the files in this application's storage dir.
 * 
 * @author wavenger
 *
 */
public class FileViewer extends ScrollView 
{
	private Context context;
	private File dir;
	private LinearLayout scrollChild;
	
	public FileViewer(Context myContext) 
	{
		super(myContext);
		setup(myContext);
		
	}
	public FileViewer(Context myContext, AttributeSet attributes)
	{
		super(myContext, attributes);
		setup(myContext);
	}
	
	private void setup(Context myContext)
	{
		context = myContext;
		this.setHorizontalScrollBarEnabled(false);
		this.setVerticalScrollBarEnabled(true);
	}
	public void init(File myDir)
	{
		if (scrollChild != null)
		{
			this.removeView(scrollChild);
			scrollChild = null;
		}
		scrollChild = new LinearLayout(context);
		
		scrollChild.setOrientation(LinearLayout.VERTICAL);
		
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		scrollChild.setLayoutParams(params);
		this.addView(scrollChild);
		
		dir = myDir;
		
		File[] contents = dir.listFiles(new WavFilenameFilter());

		Arrays.sort(contents, new WavFilenameComparator());
		
		for (File aFile : contents)
		{
			FileViewerRow row = new FileViewerRow(context);
			scrollChild.addView(row);
			row.init(aFile);
		}
	}
	
	private class FileViewerRow extends LinearLayout 
	{
		private File file;
		private TextView label;
		private Handler labelBgHandler;
		private static final String COLOR = "color";
		
		public FileViewerRow(Context context) 
		{
			super(context);
			this.setOrientation(LinearLayout.HORIZONTAL);
			LayoutInflater inflater = LayoutInflater.from(context);
			
			inflater.inflate(R.layout.file_viewer_row, this);
		}
		
		public void init(File myFile)
		{
			file = myFile;
			
			label = (TextView) this.findViewById(R.id.label);
			
			label.setText(file.getName());
			
			this.setOnClickListener(new OnFileViewerRowClick());
			
			labelBgHandler = new Handler() 
			{
				public void handleMessage (Message colorMessage)
				{
					label.setBackgroundColor(colorMessage.getData().getInt(COLOR));
				}
			};
		}
		
		private class OnFileViewerRowClick implements OnClickListener
		{
			Thread flashJobThread;
			@Override
			public void onClick(View v) {
				MediaPlayer player = new MediaPlayer();
				try 
				{
					FileInputStream fis = new FileInputStream(file);

					flashJobThread = new Thread(new FlashJob());
				
					flashJobThread.start();
					
					player.setDataSource(fis.getFD());
					player.prepare();
					player.start();
					
				}
				catch (IOException e)
				{
					TakingDictationActivity.toastError("Cannot load file: " + file.getName());
				}
			}
			private class FlashJob implements Runnable
			{
				public void sendColorMessage(int color)
				{
					Message msg = new Message();
					Bundle msgData = new Bundle();
					msgData.putInt(COLOR, color);
					msg.setData(msgData);
					labelBgHandler.sendMessage(msg);
				}
				@Override
				public void run() 
				{
					sendColorMessage(Color.GREEN);
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						
					}
					sendColorMessage(Color.TRANSPARENT);
				}
				
			}
		}
	}
	
	private class WavFilenameFilter implements FilenameFilter
	{
		private Pattern wavPattern;
		
		public WavFilenameFilter()
		{
			wavPattern = Pattern.compile(".wav$");
		}
		
		@Override
		public boolean accept(File dir, String filename) {
			Matcher matcher = wavPattern.matcher(filename);
			if (matcher.find())
			{
				return true;
			}
			return false;
		}
		
	}
	private class WavFilenameComparator implements Comparator<File>
	{

		@Override
		public int compare(File object1, File object2) {
			
			if (object1.lastModified() == object2.lastModified())
			{
				return 0;
			}
			else if (object1.lastModified() > object2.lastModified())
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}
		
	}
}
