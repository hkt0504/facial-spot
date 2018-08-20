package acne.recoz;


import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AcneImageView extends SurfaceView implements SurfaceHolder.Callback {

	private static final int INVALID = -1;

	private static final int COLOR_LINE = Color.GREEN;
	private static final int COLOR_LINE_SELECTED = Color.RED;
	private static final int COLOR_FILL = Color.argb(100, 0, 0, 255);

	private SurfaceHolder mHolder;

	private Bitmap mBitmap;

	private OnSelectListener mListener;

	private Rect mImageRect = new Rect();
	private Rect mScreenRect = new Rect();

	private Paint mPaint = new Paint();
	private RectF mCurrentRect = new RectF();

	private static final int NONE = 0;
	private static final int READY = 1;
	private static final int DRAWING = 2;
	private int mDrawing = NONE;

	private boolean mEnableDrawing = true;

	private ArrayList<RectF> mRects = new ArrayList<RectF>();
	private int mSelectedIndex = INVALID;
	private float MOVE = 10;

	public AcneImageView(Context context) {
		super(context);
		init(context);
	}

	public AcneImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AcneImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {

		float dpi = getResources().getDisplayMetrics().density;
		mHolder = this.getHolder();
		mHolder.addCallback(this);

		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(3 * dpi);
		mPaint.setDither(true);

		mDrawing = NONE;
		MOVE = dpi * 10;
	}

	public void setOnSelectListener(OnSelectListener listener) {
		mListener = listener;
	}

	public void setEnableDrawing(boolean enable) {
		mEnableDrawing = enable;
	}

	public void setImageBitmap(Bitmap bitmap) {
		mBitmap = bitmap;
		if (mBitmap != null) {
			mImageRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
			arrangeLayout();
		}
		mRects.clear();
		draw();
	}

	public void setRectsInImage(int[] rects, int count) {
		for (int i = 0; i < count; i++) {
			mRects.add(img2scr(new RectF(rects[i * 4], rects[i * 4 + 1], rects[i * 4 + 2], rects[i * 4 + 3])));
		}
		draw();
	}

	public void clearRects() {
		mRects.clear();
		draw();
	}

	public Bitmap getImage() {
		return mBitmap;
	}

	public ArrayList<RectF> getCroppedImages() {
		if (mRects.size() == 0)
			return null;

		ArrayList<RectF> rects = new ArrayList<RectF>();
		for (RectF rect : mRects) {
			RectF orgRect = scr2img(rect);
			if (orgRect != null)
				rects.add(orgRect);
		}
		return rects;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		final int pointerCount = event.getPointerCount();
		if (pointerCount == 1) {
			int action = event.getActionMasked();
			float x = event.getX(0);
			float y = event.getY(0);

			switch (action) {
			case MotionEvent.ACTION_DOWN:
				mCurrentRect.left = x;
				mCurrentRect.top = y;
				if (checkPoint(x, y)) {
					mDrawing = READY;
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mDrawing != NONE) {
					float dx = Math.abs(x - mCurrentRect.left);
					float dy = Math.abs(y - mCurrentRect.top);

					if (mDrawing == READY) {
						if (mEnableDrawing) {
							if (dx > MOVE || dy > MOVE) {
								mDrawing = DRAWING;
								if (checkPoint(x, y)) {
									mCurrentRect.right = x;
									mCurrentRect.bottom = y;
									draw();
								}
							}
						}
					} else {
						if (dx > 0 || dy > 0) {
							if (checkPoint(x, y)) {
								mCurrentRect.right = x;
								mCurrentRect.bottom = y;
								draw();
							}
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mDrawing == READY) {
					mDrawing = NONE;
					selectRect(findRect(mCurrentRect.left, mCurrentRect.top));
				} else if (mDrawing == DRAWING) {
					mDrawing = NONE;
					mRects.add(new RectF(mCurrentRect));
					draw();
				}
				break;
			}
		} else {
			mDrawing = NONE;
		}

		return false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		draw();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		if (mBitmap != null) {
			Rect dstRectOrg = new Rect(mScreenRect);

			// recalculate mDstRect
			arrangeLayout();

			for (RectF rect : mRects) {
				transformRect(rect, dstRectOrg, mScreenRect);
			}

			draw();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private void arrangeLayout() {

		int imgWidth = mImageRect.width();
		int imgHeight = mImageRect.height();

		int vWidth =  getWidth();
		int vHeight = getHeight();

		int width = vWidth - 10;
		int height = vHeight - 10;

		if (imgWidth * height > width * imgHeight) {
			height = imgHeight * width / imgWidth;
		} else {
			width = imgWidth * height / imgHeight;
		}

		int l = (vWidth - width) / 2;
		int t = (vHeight - height) / 2;

		mScreenRect.set(l, t, l + width, t + height);
	}

	private void transformRect(RectF rect, Rect srcRect, Rect dstRect) {
		float width = srcRect.width();
		float height = srcRect.height();
		if (width > 0 && height > 0) {
			float ratioX = (float)dstRect.width() / width;
			float ratioY = (float)dstRect.height() / height;
			rect.offset(-srcRect.left, -srcRect.top);
			rect.left*= ratioX;
			rect.right*= ratioX;
			rect.top *= ratioY;
			rect.bottom *= ratioY;
			rect.offset(dstRect.left, dstRect.top);
		}
	}

	private boolean checkPoint(float x, float y) {
		return mScreenRect.contains((int)x, (int)y);
	}

	private RectF img2scr(RectF rc) {
		rc.left = (rc.left * mScreenRect.width() / mImageRect.width()) + mScreenRect.left;
		rc.top = (rc.top * mScreenRect.height() / mImageRect.height()) + mScreenRect.top;
		rc.right = (rc.right * mScreenRect.width() / mImageRect.width()) + mScreenRect.left;
		rc.bottom = (rc.bottom * mScreenRect.height() / mImageRect.height()) + mScreenRect.top;
		return rc;
	}

	private RectF scr2img(RectF rc) {
		RectF rect = new RectF(rc);
		rect.left -= mScreenRect.left;
		rect.top -= mScreenRect.top;
		rect.right -= mScreenRect.left;
		rect.bottom -= mScreenRect.top;

		int width = mScreenRect.width();
		int height = mScreenRect.height();

		if (width > 0 && height > 0) {
			rect.left = rect.left * mImageRect.width() / width;
			rect.top = rect.top * mImageRect.height() / height;
			rect.right = rect.right * mImageRect.width() / width;
			rect.bottom = rect.bottom * mImageRect.height() / height;
		}
		
		return rect;
	}

	// select methods
	private int findRect(float x, float y) {
		int index = 0;
		for (RectF rect : mRects) {
			if (rect.contains(x, y)) {
				return index;
			}
			index ++;
		}
		return INVALID;
	}

	private void selectRect(int index) {
		if (index != mSelectedIndex) {
			mListener.onSelectChanged(index != INVALID);
			mSelectedIndex = index;
			draw();
		}
	}

	public void removeSelected() {
		if (mSelectedIndex != INVALID) {
			mRects.remove(mSelectedIndex);
			selectRect(INVALID);
		}
	}

	// draw mothods.
	private void draw() {
		Canvas canvas = mHolder.lockCanvas();
		if (canvas != null) {
			drawImage(canvas);
			drawRects(canvas);
			if(!mEnableDrawing)
				drawTexts(canvas);

			mHolder.unlockCanvasAndPost(canvas);
		}
	}


	private void drawImage(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		if (mBitmap != null) {
			canvas.drawBitmap(mBitmap, mImageRect, mScreenRect, null);
		}
	}

	private void drawRects(Canvas canvas) {
		final Paint paint = mPaint;

		for (RectF rect : mRects) {
			paint.setColor(COLOR_FILL);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRect(rect, paint);

			paint.setColor(COLOR_LINE);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(rect, paint);
		}

		if (mDrawing == DRAWING) {
			paint.setColor(COLOR_FILL);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRect(mCurrentRect, paint);

			paint.setColor(COLOR_LINE);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(mCurrentRect, paint);
		}

		if (mSelectedIndex != INVALID) {
			paint.setColor(COLOR_LINE_SELECTED);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(mRects.get(mSelectedIndex), paint);
		}
	}
	
	private void drawTexts(Canvas canvas) {
		final Paint paint = mPaint;

		for (RectF rect : mRects) {
			
			int width = (int)(rect.right-rect.left);
			int height = (int)(rect.bottom - rect.top);
			String strSize = String.valueOf(width) + "/" + String.valueOf(height);
			paint.setColor(Color.BLUE);
			paint.setTextSize(40);
			canvas.drawText(strSize, rect.centerX(), rect.centerY(), paint);
		}
	}

	// image
	public Bitmap getImageInRect(RectF rect) {

		Rect crop = new Rect((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom);

		try {
			if (crop.width() > 0 && crop.height() > 0)
				return Bitmap.createBitmap(mBitmap, crop.left, crop.top, crop.width(), crop.height());
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	public interface OnSelectListener {
		void onSelectChanged(boolean selected);
	}
}

