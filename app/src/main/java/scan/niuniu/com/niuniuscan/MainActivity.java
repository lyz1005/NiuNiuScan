package scan.niuniu.com.niuniuscan;

import java.io.IOException;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;

import scan.niuniu.com.niuniuscan.camera.CameraManager;
import scan.niuniu.com.niuniuscan.decoding.CaptureActivityHandler;

public class MainActivity extends Activity  {

    private CaptureActivityHandler handler;
    private ScanView scanView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private SurfaceHolder surfaceHolder;
    private final View.OnClickListener viewOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                //FIXME 优化开关闪光灯
                case R.id.flash_light:
                    Object tag = v.getTag();
                    if (tag == null || !Boolean.valueOf(tag.toString())) {
                        CameraManager.getInstance().openFlashLight();
                        v.setTag(true);
                    } else {
                        CameraManager.getInstance().closeFlashLight();
                        v.setTag(false);
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化 CameraManager
        CameraManager.init(getApplication());


        scanView = (ScanView) findViewById(R.id.scan_view);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        hasSurface = false;
        findViewById(R.id.flash_light).setOnClickListener(viewOnClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(new CallBackImpl());
        }
        decodeFormats = null;
        characterSet = null;

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.getInstance().closeDriver();
    }

    private final class CallBackImpl implements  Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!hasSurface) {
                hasSurface = true;
                initCamera(holder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            hasSurface = false;
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.getInstance().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet, scanView.getResultPointCallback());
        }
    }

//    public ScanView getViewfinderView() {
//        return scanView;
//    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        scanView.invalidate();
    }

    public void handleDecode(final Result obj) {
        Log.i("lyz", "handleDecode");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("扫描结果");
        dialog.setMessage(obj.getText());
        dialog.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用默认浏览器打开扫描得到的地址
                try {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(obj.getText());
                    intent.setData(content_url);
                    startActivity(intent);
//                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.setPositiveButton("取消", null);
        dialog.create().show();
    }
}
