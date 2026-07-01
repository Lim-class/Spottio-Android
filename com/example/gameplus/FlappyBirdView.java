package com.example.gameplus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBirdView extends View {
    private Bitmap sfondo, uccello, base, imgGameOver, imgTubo;
    private int birdX = 100, birdY = 500, velocity = 0;
    private int gravity = 2;
    private boolean isGameOver = false;
    private int score = 0;

    private Handler handler;
    private Runnable runnable;

    private ArrayList<Rect> tubiSopra, tubiSotto;
    private int gap = 450;
    private int velocitaTubi = 12;
    private Random random;

    private int highScore = 0;
    private android.content.SharedPreferences prefs;

    public FlappyBirdView(Context context) {
        super(context);
        // Caricamento Asset (Assicurati che i file esistano in res/drawable)
        sfondo = BitmapFactory.decodeResource(getResources(), R.drawable.sfondo);
        uccello = BitmapFactory.decodeResource(getResources(), R.drawable.uccello);
        base = BitmapFactory.decodeResource(getResources(), R.drawable.base);
        imgGameOver = BitmapFactory.decodeResource(getResources(), R.drawable.gameover);
        imgTubo = BitmapFactory.decodeResource(getResources(), R.drawable.tubo);

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highscore", 0);

        random = new Random();
        tubiSopra = new ArrayList<>();
        tubiSotto = new ArrayList<>();

        handler = new Handler(android.os.Looper.getMainLooper());
        runnable = this::invalidate;

        inizializzaTubi();
    }

    private void inizializzaTubi() {
        int x = 1000;
        for (int i = 0; i < 3; i++) {
            aggiungiTubo(x);
            x += 700; // Spazio orizzontale tra tubi
        }
    }

    private void aggiungiTubo(int x) {
        int h = random.nextInt(500) + 200;
        tubiSopra.add(new Rect(x, 0, x + 180, h));
        tubiSotto.add(new Rect(x, h + gap, x + 180, 2500));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Disegna Sfondo
        canvas.drawBitmap(sfondo, null, new Rect(0, 0, getWidth(), getHeight()), null);

        if (!isGameOver) {
            // 2. Logica Gravità
            velocity += gravity;
            birdY += velocity;

            // Collisione con il suolo o soffitto
            if (birdY > getHeight() - 200 - uccello.getHeight() || birdY < 0) {
                isGameOver = true;
            }

            // 3. Logica Tubi
            for (int i = 0; i < tubiSopra.size(); i++) {

                Rect sopra = tubiSopra.get(i);
                Rect sotto = tubiSotto.get(i);

                // Sposta i tubi a sinistra sottraendo la velocità alla coordinata X
                sopra.left -= velocitaTubi;
                sopra.right -= velocitaTubi;
                sotto.left -= velocitaTubi;
                sotto.right -= velocitaTubi;

                if (imgTubo != null) {
                    // Tubo SOPRA (ruotato di 180 gradi per mostrare la "testa" del tubo in basso)
                    canvas.save();
                    canvas.rotate(180, sopra.centerX(), sopra.centerY());
                    canvas.drawBitmap(imgTubo, null, sopra, null);
                    canvas.restore();

                    // Tubo SOTTO (normale)
                    canvas.drawBitmap(imgTubo, null, sotto, null);
                } else {
                    // Rettangoli verdi se l'immagine non viene caricata (evita il crash)
                    Paint pTubo = new Paint();
                    pTubo.setColor(Color.GREEN);
                    canvas.drawRect(sopra, pTubo);
                    canvas.drawRect(sotto, pTubo);
                }

                // Se il tubo esce completamente dallo schermo a sinistra
                if (sopra.right < 0) {
                    int xRecycle = 0;
                    // Trova la posizione X del tubo più a destra per accodare quello riciclato
                    for(Rect r : tubiSopra) if(r.left > xRecycle) xRecycle = r.left;

                    int h = random.nextInt(500) + 200;
                    // Riposiziona il tubo in coda con una nuova altezza casuale
                    sopra.set(xRecycle + 700, 0, xRecycle + 700 + 180, h);
                    sotto.set(xRecycle + 700, h + gap, xRecycle + 700 + 180, 2500);

                    score++; // Incrementa il punteggio perché il tubo è stato superato
                }

                // Controllo delle collisioni tra l'uccellino e i tubi
                Rect birdRect = new Rect(birdX, birdY, birdX + uccello.getWidth(), birdY + uccello.getHeight());
                if (Rect.intersects(birdRect, sopra) || Rect.intersects(birdRect, sotto)) {
                    isGameOver = true; // Fine della partita in caso di impatto
                }
            }
            // Pianifica il prossimo frame
            handler.postDelayed(runnable, 20);
        }

        // 4. Disegna Uccello e Base
        canvas.drawBitmap(uccello, birdX, birdY, null);
        canvas.drawBitmap(base, null, new Rect(0, getHeight() - 200, getWidth(), getHeight()), null);

        // 5. Testo Punteggio in tempo reale
        Paint paintScore = new Paint();
        paintScore.setColor(Color.WHITE);
        paintScore.setTextSize(80);
        canvas.drawText("Punti: " + score, 50, 150, paintScore);

        // 6. Schermata Game Over
        if (isGameOver) {
            if (score > highScore) {
                highScore = score;
                prefs.edit().putInt("highscore", highScore).apply();
            }

            int xGameOver = (getWidth() / 2) - (imgGameOver.getWidth() / 2);
            int yGameOver = (getHeight() / 2) - (imgGameOver.getHeight() / 2);
            canvas.drawBitmap(imgGameOver, xGameOver, yGameOver, null);

            Paint paintEnd = new Paint();
            paintEnd.setColor(Color.WHITE);
            paintEnd.setTextSize(60);
            paintEnd.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SCORE: " + score, getWidth() / 2, yGameOver + imgGameOver.getHeight() + 80, paintEnd);
            paintEnd.setColor(Color.YELLOW);
            canvas.drawText("BEST: " + highScore, getWidth() / 2, yGameOver + imgGameOver.getHeight() + 160, paintEnd);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                // Reset
                birdY = 500;
                velocity = 0;
                score = 0;
                isGameOver = false;
                tubiSopra.clear();
                tubiSotto.clear();
                inizializzaTubi();
                invalidate();
            } else {
                velocity = -30; // Salto più deciso
            }
        }
        return true;
    }
}