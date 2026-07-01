package com.example.gameplus;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Random;

public class TrisActivity extends AppCompatActivity {

    private Button[] buttons = new Button[9];
    private String[] board = new String[9];
    private boolean playerTurn = true; // true = X, false = O
    private String livelloDifficolta = "Facile";
    private boolean isVsPC = true; // Nuova variabile per gestire la modalità
    private TextView statusText;
    private boolean gameActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tris);

        statusText = findViewById(R.id.status_text);
        Spinner spinner = findViewById(R.id.spinner_difficolta);
        Button btnReset = findViewById(R.id.btn_reset);

        for (int i = 0; i < 9; i++) {
            String buttonID = "btn_" + i;
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            buttons[i] = findViewById(resID);
            final int index = i;
            buttons[i].setOnClickListener(v -> onCellClick(index));
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Due Giocatori")) {
                    isVsPC = false;
                } else {
                    isVsPC = true;
                    livelloDifficolta = selected;
                }
                resetGame();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnReset.setOnClickListener(v -> resetGame());
    }

    private void onCellClick(int index) {
        if (gameActive && board[index] == null) {
            // Se è vs PC, solo il giocatore X può cliccare quando è il suo turno
            // Se è vs Umano, entrambi possono cliccare a turno
            if (isVsPC && !playerTurn) return;

            String currentSign = playerTurn ? "X" : "O";
            eseguiMossa(index, currentSign);

            if (gameActive) {
                playerTurn = !playerTurn;

                if (isVsPC && !playerTurn) {
                    statusText.setText("Turno del Computer...");
                    new Handler().postDelayed(this::mossaComputer, 800);
                } else {
                    statusText.setText("Turno di " + (playerTurn ? "X" : "O"));
                }
            }
        }
    }

    private void eseguiMossa(int index, String segno) {
        board[index] = segno;
        buttons[index].setText(segno);
        buttons[index].setTextColor(segno.equals("X") ? Color.parseColor("#E94560") : Color.parseColor("#4CC9F0"));

        if (controllaVittoria(segno)) {
            statusText.setText("Vittoria: " + segno + "!");
            gameActive = false;
        } else if (isBoardFull()) {
            statusText.setText("Pareggio!");
            gameActive = false;
        }
    }

    private void mossaComputer() {
        if (!gameActive) return;

        int mossa = -1;
        if (livelloDifficolta.equals("Facile")) {
            mossa = mossaCasuale();
        } else if (livelloDifficolta.equals("Medio")) {
            mossa = (new Random().nextInt(10) < 4) ? mossaCasuale() : mossaMigliore();
        } else {
            mossa = mossaMigliore();
        }

        if (mossa != -1) {
            eseguiMossa(mossa, "O");
            if (gameActive) {
                playerTurn = true;
                statusText.setText("Turno di X");
            }
        }
    }

    private int mossaCasuale() {
        ArrayList<Integer> libere = new ArrayList<>();
        for (int i = 0; i < 9; i++) if (board[i] == null) libere.add(i);
        return libere.isEmpty() ? -1 : libere.get(new Random().nextInt(libere.size()));
    }

    private int mossaMigliore() {
        int bestVal = Integer.MIN_VALUE;
        int mossa = -1;
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = "O";
                int moveVal = minimax(0, false);
                board[i] = null;
                if (moveVal > bestVal) {
                    mossa = i;
                    bestVal = moveVal;
                }
            }
        }
        return mossa;
    }

    private int minimax(int depth, boolean isMax) {
        if (controllaVittoria("O")) return 10 - depth;
        if (controllaVittoria("X")) return depth - 10;
        if (isBoardFull()) return 0;

        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i] == null) {
                    board[i] = "O";
                    best = Math.max(best, minimax(depth + 1, false));
                    board[i] = null;
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i] == null) {
                    board[i] = "X";
                    best = Math.min(best, minimax(depth + 1, true));
                    board[i] = null;
                }
            }
            return best;
        }
    }

    private boolean controllaVittoria(String s) {
        int[][] winPositions = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] p : winPositions) {
            if (s.equals(board[p[0]]) && s.equals(board[p[1]]) && s.equals(board[p[2]])) return true;
        }
        return false;
    }

    private boolean isBoardFull() {
        for (String s : board) if (s == null) return false;
        return true;
    }

    private void resetGame() {
        board = new String[9];
        gameActive = true;
        playerTurn = true;
        statusText.setText("Turno di X");
        for (Button b : buttons) b.setText("");
    }
}