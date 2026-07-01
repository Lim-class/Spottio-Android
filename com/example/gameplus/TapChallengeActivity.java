package com.example.gameplus;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TapChallengeActivity extends AppCompatActivity {

    private TextView tvTimer, tvScore;
    private Button btnTap, btnRestart;
    private int score = 0;
    private boolean gameActive = false;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_challenge);

        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        btnTap = findViewById(R.id.btn_tap);
        btnRestart = findViewById(R.id.btn_restart);

        btnTap.setOnClickListener(v -> {
            if (!gameActive) {
                startGame();
            }
            if (gameActive) {
                score++;
                tvScore.setText("Punti: " + score);
            }
        });

        btnRestart.setOnClickListener(v -> resetGame());
    }

    private void startGame() {
        gameActive = true;
        score = 0;
        tvScore.setText("Punti: 0");
        btnRestart.setVisibility(View.GONE);

        timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Tempo: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                gameActive = false;
                tvTimer.setText("Tempo scaduto!");
                btnTap.setEnabled(false);
                btnRestart.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void resetGame() {
        btnTap.setEnabled(true);
        tvTimer.setText("Tempo: 10s");
        tvScore.setText("Punti: 0");
        btnRestart.setVisibility(View.GONE);
    }
}