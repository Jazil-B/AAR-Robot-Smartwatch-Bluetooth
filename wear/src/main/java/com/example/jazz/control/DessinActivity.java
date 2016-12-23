package com.example.jazz.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

public class DessinActivity extends SurfaceView implements SurfaceHolder.Callback {
    // Le holder
    SurfaceHolder mSurfaceHolder;
    // Le thread dans lequel le dessin se fera
    DrawingThread mThread;

    public DessinActivity (Context context) {
        super(context);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mThread = new DrawingThread();
    }


    protected void nDraw(Canvas pCanvas) {
        // Dessinez ici !
        pCanvas.drawRGB(0, 0, 0);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Nous allons dessiner nos points par rapport à la résolution de l'écran
        int iWidth = pCanvas.getWidth(); // Largeur
        int iHeight = pCanvas.getHeight(); // Hauteur

        Random rand = new Random();
        //Affichons 100 segments de toutes les couleurs
        for (int i=0; i < 100; i++)
        {
            // Affecter une couleur de manière aléatoire
            paint.setARGB(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            // Définir l'épaisseur du segment
            paint.setStrokeWidth (rand.nextInt(10));
            // Puis dessiner nos points dans le cavenas
            pCanvas.drawLine(rand.nextInt(iWidth), rand.nextInt(iHeight), rand.nextInt(iWidth), rand.nextInt(iHeight), paint);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Que faire quand le surface change ? (L'utilisateur tourne son téléphone par exemple)
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread.keepDrawing = true;
        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.keepDrawing = false;

        boolean joined = false;
        while (!joined) {
            try {
                mThread.join();
                joined = true;
            } catch (InterruptedException e) {}
        }
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
}