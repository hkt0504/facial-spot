package acne.recoz;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class ResultActivity extends Activity {
	
	private ImageView image_view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.result);
		image_view = (ImageView) findViewById(R.id.result_view);
		image_view.setImageBitmap(Common.bitmap);
	}
}
