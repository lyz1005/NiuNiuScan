package scan.niuniu.com.niuniuscan;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;


public final class ScanView extends View {

    private static final long ANIMATION_DELAY = 100L;
    private static final int OPAQUE = 0xFF;

    private DrawScanLaser drawScanLaser;
    private DrawScanMask drawScanMask;
//    private DrawScanPoint drawScanPoint;
    private int measuredWidth;
    private int measuredHeight;

    private final Rect frame = new Rect();
    ;

    private final ResultPointCallback resultPointCallback = new ResultPointCallback() {

        @Override
        public void foundPossibleResultPoint(ResultPoint point) {
//        	Log.i("lyz", "point x:"+point.getX()+";point y:"+point.getY());
//			drawScanPoint.addPoint(point);
        }
    };

    // This constructor is used when the class is built from an XML resource.
    public ScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Initialize these once for performance rather than calling them every
        // time in onDraw().
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Scan);
        if (ta != null) {
            int cornorColor = ta.getColor(R.styleable.Scan_cornor_color, R.color.scan_frame);
            int cornorLength = ta.getDimensionPixelOffset(R.styleable.Scan_cornor_length, R.dimen.corner_length);
            int cornorWidth = ta.getDimensionPixelOffset(R.styleable.Scan_cornor_width, R.dimen.corner_width);
            int cornorMargin = ta.getDimensionPixelOffset(R.styleable.Scan_cornor_margin, R.dimen.corner_length);
            CornorProperty cornorProperty = new CornorProperty(cornorWidth, cornorLength, cornorMargin, cornorColor);
            int frameColor = ta.getColor(R.styleable.Scan_frame_color, R.color.scan_frame);
            int maskColor = ta.getColor(R.styleable.Scan_mask_color, R.color.scan_mask);
            int laserId = ta.getResourceId(R.styleable.Scan_laser, R.mipmap.scan_laser);
            drawScanMask = new DrawScanMask(cornorProperty, frameColor, maskColor);
            drawScanLaser = new DrawScanLaser(laserId, 3000, cornorMargin + cornorWidth);
            ta.recycle();
        } else {
            Resources resources = getResources();
            CornorProperty cornorProperty = CornorProperty.createDefault(resources);
            drawScanMask = new DrawScanMask(cornorProperty, resources.getColor(R.color.scan_frame), resources.getColor(R.color.scan_mask));
            drawScanLaser = new DrawScanLaser(R.mipmap.scan_laser, 3000, cornorProperty.cornorMargin + cornorProperty.cornorWidth);
        }
//        drawScanPoint = new DrawScanPoint(getResources());
    }


    private static final class CornorProperty {
        private final int cornorWidth;
        private final int cornorLength;
        private final int cornorMargin;
        private final int cornorColor;

        public CornorProperty(int cornorWidth, int cornorLength,int cornorMargin, int cornorColor) {
            this.cornorWidth = cornorWidth;
            this.cornorLength = cornorLength;
            this.cornorMargin = cornorMargin;
            this.cornorColor = cornorColor;
        }

        private static CornorProperty createDefault(Resources res) {
            int cornorWidth = res.getDimensionPixelOffset(R.dimen.corner_width);
            int cornorLength = res.getDimensionPixelOffset(R.dimen.corner_length);
            int cornorMargin = res.getDimensionPixelOffset(R.dimen.corner_margin);
            int cornorColor = res.getColor(R.color.scan_frame);
            return new CornorProperty(cornorWidth, cornorLength, cornorMargin, cornorColor);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredWidth = w;
        measuredHeight = h;
        frame.set(createFrameRect(measuredWidth, measuredHeight));
        drawScanLaser.onSizeChange();
        drawScanMask.onSizeChange();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Draw a red "laser scanner" line through the middle to show
        // decoding is active
        drawScanLaser.doDraw(canvas);
//		drawScanPoint.doDraw(canvas);
        drawScanMask.doDraw(canvas);

        // Request another update at the animation interval, but only
        // repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
                frame.right, frame.bottom);

    }

    public ResultPointCallback getResultPointCallback() {
        return resultPointCallback;
    }

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 720;
    private static final int MAX_FRAME_HEIGHT = 480;

    private Rect createFrameRect(int viewWidth, int viewHeight) {
        int width = viewWidth * 3 / 4;
        int height = viewHeight * 3 / 4;
        if (width < MIN_FRAME_WIDTH) {
            width = MIN_FRAME_WIDTH;
        } else if (width > MAX_FRAME_WIDTH) {
            width = MAX_FRAME_WIDTH;
        }
        if (height < MIN_FRAME_HEIGHT) {
            height = MIN_FRAME_HEIGHT;
        } else if (height > MAX_FRAME_HEIGHT) {
            height = width / 3 * 2;
        }
        int leftOffset = (viewWidth - width) / 2;
        int topOffset = (measuredHeight - height) / 2;
        Rect framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
                topOffset + height);

        Log.i("rect",String.format("width%d,height%d",framingRect.width(),framingRect.height()));
        return framingRect;
    }

    /**
     * ����ɨ�輫����
     */
    private final class DrawScanLaser {
        private final Paint paint = new Paint();
        private Bitmap bitmap;
        private final int maxEachScanTime;
        private int length;
        private long initialTime = -1;
        private int laserId;
        private final int space;

        private DrawScanLaser(int laserId, int maxEachScanTime, int space) {
            this.laserId = laserId;
            this.maxEachScanTime = maxEachScanTime;
            onSizeChange();
            this.space = space;
        }

//        private Bitmap calculatebitmapSize(Bitmap bitmap, int width, int height) {
//            return Bitmap.createBitmap(bitmap, 0, 0, width, height);
//        }

        private void onSizeChange() {
            int width = frame.width();
            if (width == 0)
                return;
            bitmap = BitmapFactory.decodeResource(getResources(), laserId);
            this.length = frame.bottom - frame.top - space * 2 - bitmap.getHeight();
//            bitmap = calculatebitmapSize(tempBitMap, width - this.space * 2, tempBitMap.getHeight());
//            tempBitMap.recycle();
        }

        public void doDraw(Canvas canvas) {
            if (bitmap == null)
                return;
            long elapsedTime = adjustElapsedTime();
            float per = elapsedTime * 1.0f / maxEachScanTime;
            float top = frame.top + length * per;
            canvas.drawBitmap(bitmap, frame.left + this.space, top, paint);
        }

        private long adjustElapsedTime() {
            if (initialTime == -1) {
                initialTime = SystemClock.elapsedRealtime();
            }
            long currentRealtime = SystemClock.elapsedRealtime();
            long elapsedTime = currentRealtime - initialTime;
            if (elapsedTime > maxEachScanTime) {
                initialTime = -1;
            }
            if (elapsedTime == 0) {
                return 1;
            }
            return elapsedTime;
        }
    }

    /**
     * ��������
     */
    private final class DrawScanMask {
        private final Paint paint = new Paint();
        private final Paint frameLinePaint = new Paint();
        private Path maskPath = new Path();
        private final Paint frameCornerPaint = new Paint();

        private Path framePath = new Path();
        private CornorProperty cornorProperty;

        public DrawScanMask(CornorProperty cornorProperty, int frameColor, int maskColor) {
            this.cornorProperty = cornorProperty;
            paint.setColor(maskColor);
            frameLinePaint.setColor(frameColor);
            frameLinePaint.setStyle(Style.STROKE);
            frameCornerPaint.setColor(cornorProperty.cornorColor);
            frameCornerPaint.setStrokeWidth(cornorProperty.cornorWidth);
            frameCornerPaint.setStyle(Style.STROKE);
            onSizeChange();
        }

        private void onSizeChange() {
            framePath = new Path();
            int cornorMargin = cornorProperty.cornorMargin + cornorProperty.cornorWidth / 2;
            drawVerticalCornerLine(frame.left + cornorMargin, cornorMargin);
            drawVerticalCornerLine(frame.right - cornorMargin, cornorMargin);
            drawHorizontalCornerLine(frame.top + cornorMargin, cornorMargin);
            drawHorizontalCornerLine(frame.bottom - cornorMargin, cornorMargin);
            framePath.close();

            RectF rect = new RectF(0, 0, measuredWidth, measuredHeight);
            maskPath.addRect(new RectF(frame), Path.Direction.CCW);
            maskPath.addRect(rect, Path.Direction.CW);
            maskPath.close();
        }

        private void drawVerticalCornerLine(float x, int cornorMargin) {
            int cornorLength = cornorProperty.cornorLength;
            int topY = frame.top + cornorMargin - cornorProperty.cornorWidth / 2;
            framePath.moveTo(x, topY);
            framePath.lineTo(x, topY + cornorLength);

            int bottomY = frame.bottom - cornorMargin + cornorProperty.cornorWidth / 2;
            framePath.moveTo(x, bottomY - cornorLength);
            framePath.lineTo(x, bottomY);
        }

        private void drawHorizontalCornerLine(float y, int cornorMargin) {
            int cornorLength = cornorProperty.cornorLength;
            int leftX = frame.left + cornorMargin - cornorProperty.cornorWidth / 2;
            framePath.moveTo(leftX, y);
            framePath.lineTo(leftX + cornorLength, y);

            int rightX = frame.right - cornorMargin + cornorProperty.cornorWidth / 2;
            framePath.moveTo(rightX - cornorLength, y);
            framePath.lineTo(rightX, y);
        }

        public void doDraw(Canvas canvas) {
            canvas.drawPath(maskPath, paint);
            canvas.drawRect(frame, frameLinePaint);
            canvas.drawPath(framePath, frameCornerPaint);
        }
    }

//    /**
//     * ����ɨ���
//     */
//    private final class DrawScanPoint {
//        private final Paint paint = new Paint();
//        private Collection<ResultPoint> possibleResultPoints = new HashSet<ResultPoint>(5);
//        private Collection<ResultPoint> lastPossibleResultPoints = new HashSet<ResultPoint>(5);
//
//        private DrawScanPoint(Resources resources) {
//            int resultPointColor = resources.getColor(R.color.possible_result_points);
//            paint.setColor(resultPointColor);
//        }
//
//        public void doDraw(Canvas canvas) {
////			Collection<ResultPoint> currentPossible = possibleResultPoints;
////			Collection<ResultPoint> currentLast = lastPossibleResultPoints;
////			lastPossibleResultPoints.clear();
////			if (!currentPossible.isEmpty()) {
////				possibleResultPoints.clear();
////				lastPossibleResultPoints.addAll(currentPossible);
////				paint.setAlpha(OPAQUE);
////				drawPoint(canvas,currentPossible, 6.0f);
////			}
////			if (!currentLast.isEmpty()) {
////				paint.setAlpha(OPAQUE / 2);
////				drawPoint(canvas,currentLast, 3.0f);
////			}
//
////			Collection<ResultPoint> currentPossible = possibleResultPoints;
////			Collection<ResultPoint> currentLast = lastPossibleResultPoints;
//            if (!possibleResultPoints.isEmpty()) {
//                paint.setAlpha(OPAQUE / 2);
//                drawPoint(canvas, lastPossibleResultPoints, 3.0f);
//            }
//            if (possibleResultPoints.isEmpty()) {
//                lastPossibleResultPoints.clear();
//            } else {
//                lastPossibleResultPoints.addAll(possibleResultPoints);
//                possibleResultPoints.clear();
//                paint.setAlpha(OPAQUE);
//                drawPoint(canvas, possibleResultPoints, 6.0f);
//            }
//        }
//
//        public void addPoint(ResultPoint point) {
//            possibleResultPoints.add(point);
//        }
//
//        private void drawPoint(Canvas canvas, Collection<ResultPoint> points, float radius) {
//            for (ResultPoint point : points) {
//                canvas.drawCircle(frame.left + point.getX(),
//                        frame.top + point.getY(), radius, paint);
//            }
//        }
//    }

}
