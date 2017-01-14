package com.example.jazz.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback, MainActivity.GraphDrawing {
    // Le holder
    SurfaceHolder mSurfaceHolder;
    // Le thread dans lequel le dessin se fera
    DrawingThread mThread;
    private boolean fp = false;

    private float cursorX, cursorY;
    private int graphDirection = -1;

    private ArrayList<Graph> graphPaths;

    private Bitmap imgUp;
    private Bitmap imgDown;
    private Bitmap imgLeft;
    private Bitmap imgRight;
    private Bitmap imgTurbo;
    private Bitmap imgTurboDown;
    private Bitmap imgController;


    public GraphView(Context context, AttributeSet attrs) {
        super(context,attrs);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mThread = new DrawingThread();

        //Lien des images
        imgUp = BitmapFactory.decodeResource(getResources(), R.drawable.up);
        imgDown = BitmapFactory.decodeResource(getResources(), R.drawable.down);
        imgLeft = BitmapFactory.decodeResource(getResources(), R.drawable.left);
        imgRight = BitmapFactory.decodeResource(getResources(), R.drawable.next);

        imgTurbo = BitmapFactory.decodeResource(getResources(), R.drawable.turbo);
        imgTurboDown = BitmapFactory.decodeResource(getResources(), R.drawable.turbodown);
        imgController = BitmapFactory.decodeResource(getResources(), R.drawable.controllerx250);



    }


    private void init() {

        graphPaths = new ArrayList<>();

        cursorX = getWidth() / 2;
        cursorY = getHeight() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10, getResources().getDisplayMetrics());

        if ((mThread!=null) && (!mThread.isAlive())) {
            mThread.start();
            Log.d("-FCT-", "cv_thread.start()");
        }
    }


    protected void nDraw(Canvas pCanvas) {
        if(pCanvas != null) {
            pCanvas.drawRGB(189, 195, 199);

            switch(graphDirection) {
                case 0 : //Haut
                    pCanvas.drawBitmap(imgUp,  -30, -30, null);
                    break;
                case 1 : //Droite
                    pCanvas.drawBitmap(imgRight, -30, -30, null);
                    break;
                case 2 : //Bas
                    pCanvas.drawBitmap(imgDown,  -30, -30, null);

                    break;
                case 3 : //Gauche
                    pCanvas.drawBitmap(imgLeft,  -30, -30, null);
                    break;
                case 4 : //Haut droite
                    pCanvas.drawBitmap(imgTurbo,  -30, -30, null);
                    break;
                case 5 : //bas droite
                    pCanvas.drawBitmap(imgTurboDown, -30, -30, null);
                    break;
                default :
                    pCanvas.drawRGB(189, 195, 199);
                    pCanvas.drawBitmap(imgController, -30, -30, null);
                    break;
            }

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("-> FCT <-", "surfaceChanged");
        if(!fp) {
            init();
            fp = true;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("-> FCT <-", "surfaceCreated");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("-> FCT <-", "surfaceDestroyed");
    }

    @Override
    public void drawGraph(int dir) {
        graphDirection = dir;
    }

    private class DrawingThread extends Thread {
        // Utilisé pour arrêter le dessin quand il le faut
        boolean keepDrawing = true;

        @Override
        public void run() {

            while (keepDrawing) {
                Canvas canvas = null;
                try {
                    // On récupère le canvas pour dessiner dessus
                    canvas = mSurfaceHolder.lockCanvas();
                    // On s'assure qu'aucun autre thread n'accède au holder
                    synchronized (mSurfaceHolder) {
                        // Et on dessine
                        nDraw(canvas);
                    }
                } finally {
                    // Notre dessin fini, on relâche le Canvas pour que le dessin s'affiche
                    if (canvas != null)
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                }

                // Pour dessiner à 50 fps
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {}
            }
        }
    }


    private class Graph {
        private Path path;
        private Paint paint;

        public Graph(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }

        public Path getPath() {
            return path;
        }

        public Paint getPaint() {
            return paint;
        }
    }
}