package acne.recoz;

import android.graphics.Bitmap;
import android.os.Environment;

public class Common {
	public static Bitmap bitmap;
	public static String SAMPLE_DIR = Environment.getExternalStorageDirectory() + "/Acne/";
	public static String dataPath = Common.SAMPLE_DIR + "face.xml";
	public static int face_size = 128;
}

