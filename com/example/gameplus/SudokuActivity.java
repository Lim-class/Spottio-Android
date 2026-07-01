package com.example.gameplus;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class SudokuActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 9;
    private EditText[][] cells = new EditText[GRID_SIZE][GRID_SIZE];
    private int[][] solution = new int[GRID_SIZE][GRID_SIZE];
    private int[][] puzzle = new int[GRID_SIZE][GRID_SIZE];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        GridLayout gridLayout = findViewById(R.id.sudoku_grid);
        Button btnVerifica = findViewById(R.id.btn_verifica);
        Button btnSuggerimento = findViewById(R.id.btn_suggerimento);

        generaNuovoSudoku();
        creaInterfaccia(gridLayout);

        btnVerifica.setOnClickListener(v -> verificaSoluzione());
        btnSuggerimento.setOnClickListener(v -> daiSuggerimento());
    }

    // --- LOGICA DI GENERAZIONE (BACKTRACKING) ---
    private void generaNuovoSudoku() {
        for (int i = 0; i < GRID_SIZE; i++) Arrays.fill(solution[i], 0);

        solve(0, 0); // Riempie la griglia 'solution' in modo completo e valido

        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(solution[i], 0, puzzle[i], 0, GRID_SIZE);
        }

        // Rimuove circa 45 numeri per creare il gioco
        Random rand = new Random();
        int rimosse = 45;
        while (rimosse > 0) {
            int r = rand.nextInt(GRID_SIZE);
            int c = rand.nextInt(GRID_SIZE);
            if (puzzle[r][c] != 0) {
                puzzle[r][c] = 0;
                rimosse--;
            }
        }
    }

    private boolean solve(int row, int col) {
        if (row == GRID_SIZE) return true;
        int nextRow = (col == GRID_SIZE - 1) ? row + 1 : row;
        int nextCol = (col == GRID_SIZE - 1) ? 0 : col + 1;

        Integer[] nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Collections.shuffle(Arrays.asList(nums));

        for (int num : nums) {
            if (isValid(row, col, num)) {
                solution[row][col] = num;
                if (solve(nextRow, nextCol)) return true;
                solution[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValid(int row, int col, int num) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (solution[row][i] == num || solution[i][col] == num) return false;
        }
        int sRow = (row / 3) * 3, sCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (solution[sRow + i][sCol + j] == num) return false;
            }
        }
        return true;
    }

    private void applicaStileCella (int r, int c, EditText cella) {
        // Distingue i blocchi 3x3 con i bordi o i colori di sfondo
        if (((r / 3) + (c / 3)) % 2 == 0) {
            cella.setBackgroundColor(Color.parseColor("#E0E0E0")); // Grigio chiaro
        } else {
            cella.setBackgroundColor(Color.WHITE);
        }

        // Listener per rimuovere il colore rosso dell'errore quando l'utente corregge
        cella.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (cella.isEnabled()) cella.setTextColor(Color.BLUE);
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // --- INTERFACCIA GRAFICA ---
    private void creaInterfaccia(GridLayout gridLayout) {
        gridLayout.removeAllViews();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = (screenWidth - 100) / 9;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                cells[r][c] = new EditText(this);
                applicaStileCella(r, c, cells[r][c]); // Nuovo stile

                cells[r][c].setGravity(Gravity.CENTER);
                cells[r][c].setInputType(InputType.TYPE_CLASS_NUMBER);
                cells[r][c].setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                // Margini sottili per far vedere la griglia nera sottostante
                params.setMargins(1, 1, 1, 1);
                cells[r][c].setLayoutParams(params);

                if (puzzle[r][c] != 0) {
                    cells[r][c].setText(String.valueOf(puzzle[r][c]));
                    cells[r][c].setEnabled(false);
                    cells[r][c].setTextColor(Color.BLACK);
                    cells[r][c].setTypeface(null, Typeface.BOLD);
                } else {
                    cells[r][c].setTextColor(Color.BLUE);
                }
                gridLayout.addView(cells[r][c]);
            }
        }
    }

    // --- FUNZIONALITÀ PULSANTI ---
    private void verificaSoluzione() {
        boolean errore = false;
        boolean completo = true;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (!cells[r][c].isEnabled()) continue;

                String val = cells[r][c].getText().toString();
                if (val.isEmpty()) {
                    completo = false;
                } else {
                    if (Integer.parseInt(val) != solution[r][c]) {
                        cells[r][c].setTextColor(Color.RED);
                        errore = true;
                    } else {
                        cells[r][c].setTextColor(Color.BLUE);
                    }
                }
            }
        }

        if (errore) Toast.makeText(this, "Ci sono errori in rosso!", Toast.LENGTH_SHORT).show();
        else if (completo) Toast.makeText(this, "VITTORIA! Sudoku completato!", Toast.LENGTH_LONG).show();
        else Toast.makeText(this, "Corretto finora!", Toast.LENGTH_SHORT).show();
    }

    private void daiSuggerimento() {
        ArrayList<int[]> vuote = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                String val = cells[r][c].getText().toString();
                if (val.isEmpty() || Integer.parseInt(val) != solution[r][c]) {
                    vuote.add(new int[]{r, c});
                }
            }
        }

        if (!vuote.isEmpty()) {
            int[] scelta = vuote.get(new Random().nextInt(vuote.size()));
            int r = scelta[0], c = scelta[1];
            cells[r][c].setText(String.valueOf(solution[r][c]));
            cells[r][c].setTextColor(Color.parseColor("#800080")); // Viola suggerimento
            cells[r][c].setTypeface(null, Typeface.ITALIC);
        }
    }
}