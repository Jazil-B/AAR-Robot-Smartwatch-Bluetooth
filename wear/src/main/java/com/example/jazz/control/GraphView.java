package com.example.jazz.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

    public GraphView(Context context, AttributeSet attrs) {
        super(context,attrs);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mThread = new DrawingThread();
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
            pCanvas.drawRGB(255, 255, 255);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setAntiAlias(true);

            Path p = new Path();
            p.moveTo(cursorX,cursorY);

            switch(graphDirection) {
                case 0 : //Haut
                    paint.setColor(Color.BLUE);
                    cursorY--;
                    break;
                case 1 : //Droite
                    paint.setColor(Color.GREEN);
                    cursorX++;
                    //p.lineTo(300f, 0f);
                    break;
                case 2 : //Bas
                    paint.setColor(Color.RED);
                    cursorY++;
                    break;
                case 3 : //Gauche
                    paint.setColor(Color.rgb(255,153,0));
                    cursorX--;
                    break;
                case 4 : //Haut droite
                    paint.setColor(Color.rgb(255,153,0));
                    cursorX++;
                    cursorY--;
                    break;
                case 5 : //bas droite
                    paint.setColor(Color.rgb(255,153,0));
                    cursorX++;
                    cursorY++;
                    break;
                case 6 : //Bas gauche
                    paint.setColor(Color.rgb(255,153,0));
                    cursorX--;
                    cursorY++;
                    break;
                case 7 : //Haut gauche
                    paint.setColor(Color.rgb(255,153,0));
                    cursorX--;
                    cursorY--;
                    break;
                default : break;
            }

            p.lineTo(cursorX,cursorY);
            graphPaths.add(new Graph(p, paint));

            for(Graph g : graphPaths) {
                pCanvas.drawPath(g.getPath(),g.getPaint());
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