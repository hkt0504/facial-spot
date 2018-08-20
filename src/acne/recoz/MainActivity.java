package acne.recoz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private static final int REQUEST_GALLERY = 101;
	private static final int REQUEST_RESULT = 102;

	private Uri imgUri;
	private String imagePath;

	private ImageView image_view;
	private Button deleteBtn;

	private Bitmap bitmap = null;
	private int bmpWidth;
	private int bmpHeight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		findViewById(R.id.proc).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (bitmap == null)
				{
					Toast.makeText(MainActivity.this, "Please TakePicture/Select from Gallery first.", Toast.LENGTH_LONG).show();
				}
				else
					process();
			}
		});

		findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				openGallery();
			}
		});

		findViewById(R.id.rotate_l).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				rotate(true);
			}
		});

		findViewById(R.id.rotate_r).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				rotate(false);
			}
		});

		image_view = (ImageView) findViewById(R.id.imageView);
		
		File SAMPLE_DIR = new File(Common.SAMPLE_DIR);
		if(!SAMPLE_DIR.exists())
			SAMPLE_DIR.mkdirs();
		copyAssets();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case REQUEST_GALLERY:
			if (resultCode == RESULT_OK) {
				imagePath = getRealPathFromURI(data, null, false);
				if (imagePath == null)
					return;

				try {
					Bitmap load_bitmap = BitmapFactory.decodeFile(imagePath);
					int width = load_bitmap.getWidth();
					int height = load_bitmap.getHeight();

					if (width < height)
					{
						bmpWidth = 480;
						bmpHeight = (int)((double)bmpWidth*(double)height/(double)width);
					}
					else
					{
						bmpHeight = 480;
						bmpWidth = (int)((double)bmpHeight*(double)width/(double)height);
					}

					bitmap = Bitmap.createScaledBitmap(load_bitmap, bmpWidth, bmpHeight, false);
				} catch (OutOfMemoryError e) {
					Log.e("MainActivity", "error", e);

					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inSampleSize = 4;
					try {
						Bitmap load_bitmap = BitmapFactory.decodeFile(imagePath, opts);
						int width = load_bitmap.getWidth();
						int height = load_bitmap.getHeight();

						if (width < height)
						{
							bmpWidth = 480;
							bmpHeight = (int)((double)bmpWidth*(double)height/(double)width);
						}
						else
						{
							bmpHeight = 480;
							bmpWidth = (int)((double)bmpHeight*(double)width/(double)height);
						}

						bitmap = Bitmap.createScaledBitmap(load_bitmap, bmpWidth, bmpHeight, false);
					} catch (OutOfMemoryError e1) {
						Log.e("CropActivity", "error", e1);
						return;
					}
				}

				image_view.setImageBitmap(bitmap);
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	private String getRealPathFromURI(Intent data, String defaultPath, boolean isVideo) {
		try {
			Uri contentUri = (data == null) ? imgUri : data.getData();
			String[] proj = isVideo ? new String[] { MediaStore.Video.Media.DATA } : new String[]{ MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
			int columnIdx = cursor.getColumnIndexOrThrow(isVideo ? MediaStore.Video.Media.DATA : MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(columnIdx);
		} catch (Exception e) {
			// contentUri or cursor is null 
			return defaultPath;
		}
	}

	private void openGallery() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, REQUEST_GALLERY);
	}

	private class ImageModeProcessTask extends AsyncTask<Void, Void, Boolean> {

		private Context ctx;
		private ProgressDialog progDlg;
		private Bitmap work_bitmap;
		int acne_cnt = 0;

		public ImageModeProcessTask(Context ctx, Bitmap open_bitmap) {
			super();
			this.ctx = ctx;
			this.work_bitmap = open_bitmap;
		}

		@Override
		protected Boolean doInBackground(Void... params) {

			int width = work_bitmap.getWidth();
			int height = work_bitmap.getHeight();
			int[] outPixels = new int[width * height];
			acne_cnt = acneRecoz(width, height, work_bitmap, Common.dataPath, Common.face_size, outPixels);			

			if (acne_cnt < 1)
				return false;
			else
			{
				bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				bitmap.setPixels(outPixels, 0, width, 0, 0, width, height);
				Common.bitmap = bitmap;
				return true;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (progDlg != null)
				progDlg.dismiss();
			
			if (result)
			{
				showResult();
			}
			else
			{
				Toast.makeText(MainActivity.this, "Cannot find Acne.", Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onPreExecute() {
			progDlg = ProgressDialog.show(ctx, "Wait", "Image Processing....", false);
		}	
	}

	void rotate(boolean bLeft) {
		if (bitmap == null) {
			Toast.makeText(MainActivity.this, "Please TakePicture/Select from Galery first.", Toast.LENGTH_LONG).show();
		} else {
			int[] outPixels = new int[bmpWidth*bmpHeight];
			rotateBitmap(bmpWidth, bmpHeight, bitmap, bLeft, outPixels);

			bitmap = null;
			bitmap = Bitmap.createBitmap(bmpHeight, bmpWidth, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(outPixels, 0, bmpHeight, 0, 0, bmpHeight, bmpWidth);
			bmpWidth = bitmap.getWidth();
			bmpHeight = bitmap.getHeight();
			
			image_view.setImageBitmap(bitmap);
		}
	}

	void process() {
		
		if (bitmap == null) {
			Toast.makeText(MainActivity.this, "Please TakePicture/Select from Galery first.", Toast.LENGTH_LONG).show();
		} else {
			Bitmap processBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
			ImageModeProcessTask task = new ImageModeProcessTask(this, processBitmap);
			task.execute();
		}
	}
	
	void showResult()
	{
		Intent intent = new Intent(this, ResultActivity.class);
		this.startActivityForResult(intent, REQUEST_RESULT);
	}
	
	private void copyAssets() 
	{
	    AssetManager assetManager = getAssets();
	    String[] files = null;
	    try {
	        files = assetManager.list("");
	    } catch (IOException e) {
	        Log.e("tag", "Failed to get asset file list.", e);
	    }
	    if (files != null) for (String filename : files) {
	        InputStream in = null;
	        OutputStream out = null;
	        try {
	          in = assetManager.open(filename);
	          File outFile = new File(Common.SAMPLE_DIR, filename);
	          out = new FileOutputStream(outFile);
	          copyFile(in, out);
	        } catch(IOException e) {
	            Log.e("tag", "Failed to copy asset file: " + filename, e);
	        }     
	        finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {
	                    // NOOP
	                }
	            }
	            if (out != null) {
	                try {
	                    out.close();
	                } catch (IOException e) {
	                    // NOOP
	                }
	            }
	        }  
	    }
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}

	public native void rotateBitmap(int width, int height, Bitmap bmpOrg, boolean bLeft, int[] outPixels);
	public native int acneRecoz(int width, int height, Bitmap bmpOrg, String dataPath, int face_size, int[] outPixels);

	static {
		System.loadLibrary("opencv_java");
		System.loadLibrary("ImageProcessing");
	}
}
