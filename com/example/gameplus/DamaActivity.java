package com.example.gameplus;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DamaActivity extends AppCompatActivity {

    private FrameLayout[][] caselle = new FrameLayout[8][8];
    private boolean turnoNeri = true;
    private FrameLayout casellaSelezionata = null;
    private int rSel = -1, cSel = -1;
    private boolean multiplaInCorso = false;
    private boolean vsAI = false;

    private TextView txtStatus;
    private GridLayout grid;
    private View rootLayout;
    private Spinner spinnerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dama);

        txtStatus = findViewById(R.id.status_dama);
        grid = findViewById(R.id.dama_grid);
        rootLayout = findViewById(android.R.id.content);
        spinnerMode = findViewById(R.id.spinner_mode);

        setupSpinner();
        inizializzaScacchiera();
        aggiornaInterfacciaTurno();

        findViewById(R.id.btn_reset_dama).setOnClickListener(v -> recreate());
    }

    private void setupSpinner() {
        String[] opzioni = {"2 Giocatori", "Contro PC (Facile)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opzioni);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(adapter);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vsAI = (position == 1);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void inizializzaScacchiera() {
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = (displayWidth - 64) / 8;

        grid.removeAllViews();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                FrameLayout box = new FrameLayout(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r), GridLayout.spec(c));
                params.width = cellSize;
                params.height = cellSize;
                box.setLayoutParams(params);

                boolean isCasellaAttiva = (r + c) % 2 != 0;
                box.setBackgroundColor(isCasellaAttiva ? Color.parseColor("#B58863") : Color.parseColor("#F0D9B5"));

                if (isCasellaAttiva) setupPezzi(box, r, c);

                final int row = r, col = c;
                box.setOnClickListener(v -> gestisciTocco(row, col));
                caselle[r][c] = box;
                grid.addView(box);
            }
        }
    }

    private void setupPezzi(FrameLayout box, int r, int c) {
        if (r < 3 || r > 4) {
            ImageView pezzo = new ImageView(this);
            int pieceSize = (int) (getResources().getDisplayMetrics().widthPixels / 10);
            pezzo.setLayoutParams(new FrameLayout.LayoutParams(pieceSize, pieceSize, Gravity.CENTER));
            pezzo.setImageResource(android.R.drawable.presence_online);

            if (r < 3) {
                pezzo.setColorFilter(Color.WHITE);
                pezzo.setTag("BIANCO");
            } else {
                pezzo.setColorFilter(Color.BLACK);
                pezzo.setTag("NERO");
            }
            box.addView(pezzo);
        }
    }

    private void gestisciTocco(int r, int c) {
        if (vsAI && !turnoNeri) return; // Blocca input umano se tocca al PC

        String coloreTurno = turnoNeri ? "NERO" : "BIANCO";
        FrameLayout boxToccata = caselle[r][c];

        if (boxToccata.getChildCount() > 0) {
            if (multiplaInCorso) return;
            ImageView pezzo = (ImageView) boxToccata.getChildAt(0);
            if (pezzo.getTag().toString().startsWith(coloreTurno)) {
                selezionaPezzo(boxToccata, r, c);
            }
        } else if (casellaSelezionata != null) {
            analizzaMossa(r, c);
        }
    }

    private void selezionaPezzo(FrameLayout box, int r, int c) {
        resetColoriScacchiera();
        casellaSelezionata = box;
        rSel = r; cSel = c;
        box.setBackgroundColor(Color.parseColor("#7B9734"));
    }

    private void analizzaMossa(int r, int c) {
        int distR = r - rSel, distC = c - cSel;
        int absR = Math.abs(distR), absC = Math.abs(distC);

        if (absR != absC) return;

        String tagPezzo = casellaSelezionata.getChildAt(0).getTag().toString();
        boolean isDama = tagPezzo.contains("DAMA");

        if (absR == 2) {
            int rM = rSel + distR / 2, cM = cSel + distC / 2;
            if (isNemico(rM, cM)) {
                eseguiMossa(r, c, rM, cM);
                return;
            }
        }

        if (!multiplaInCorso && absR == 1) {
            int direzioneValida = turnoNeri ? -1 : 1;
            if (isDama || distR == direzioneValida) {
                eseguiMossa(r, c, -1, -1);
            }
        }
    }

    private boolean isNemico(int r, int c) {
        if (caselle[r][c].getChildCount() == 0) return false;
        String tag = caselle[r][c].getChildAt(0).getTag().toString();
        String alleato = turnoNeri ? "NERO" : "BIANCO";
        return !tag.startsWith(alleato);
    }

    private void eseguiMossa(int r, int c, int rM, int cM) {
        ImageView pezzo = (ImageView) casellaSelezionata.getChildAt(0);
        if (rM != -1) caselle[rM][cM].removeAllViews();

        casellaSelezionata.removeAllViews();
        caselle[r][c].addView(pezzo);

        if (!pezzo.getTag().toString().contains("DAMA")) {
            if ((turnoNeri && r == 0) || (!turnoNeri && r == 7)) {
                pezzo.setTag(turnoNeri ? "NERO_DAMA" : "BIANCO_DAMA");
                pezzo.setBackgroundResource(android.R.drawable.btn_star_big_on);
            }
        }

        rSel = r; cSel = c;
        casellaSelezionata = caselle[r][c];

        if (rM != -1 && puoAncoraMangiare(r, c)) {
            multiplaInCorso = true;
            if (!vsAI || turnoNeri) casellaSelezionata.setBackgroundColor(Color.CYAN);
            if (vsAI && !turnoNeri) new Handler().postDelayed(this::mossaAI, 800);
        } else {
            concludiTurno();
        }
    }

    private void concludiTurno() {
        multiplaInCorso = false;
        turnoNeri = !turnoNeri;
        casellaSelezionata = null;
        resetColoriScacchiera();
        aggiornaInterfacciaTurno();
        controllaVittoria();

        if (!vsAI) {
            float targetRotation = turnoNeri ? 0f : 180f;
            grid.animate().rotation(targetRotation).setDuration(600).start();
            ruotaPezzi(targetRotation);
        }

        if (vsAI && !turnoNeri) {
            new Handler().postDelayed(this::mossaAI, 1000);
        }

        if (!haMosseDisponibili(turnoNeri)) {
            Toast.makeText(this, "PAREGGIO! Nessuna mossa disponibile.", Toast.LENGTH_LONG).show();
            return;
        }

    }

    private void mossaAI() {
        List<int[]> mosseMangia = new ArrayList<>();
        List<int[]> mosseSemplici = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (caselle[r][c].getChildCount() > 0) {
                    String tag = caselle[r][c].getChildAt(0).getTag().toString();
                    if (tag.startsWith("BIANCO")) {
                        boolean isDamaAI = tag.contains("DAMA");
                        int[][] dirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
                        for (int[] d : dirs) {
                            int nr = r + d[0], nc = c + d[1];
                            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && caselle[nr][nc].getChildCount() == 0) {
                                if (Math.abs(d[0]) == 2) {
                                    if (isNemicoAI(r + d[0]/2, c + d[1]/2)) mosseMangia.add(new int[]{r, c, nr, nc, r + d[0]/2, c + d[1]/2});
                                } else if (!multiplaInCorso && (isDamaAI || d[0] == 1)) {
                                    mosseSemplici.add(new int[]{r, c, nr, nc, -1, -1});
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!mosseMangia.isEmpty()) {
            int[] m = mosseMangia.get(new Random().nextInt(mosseMangia.size()));
            applicatoAI(m);
        } else if (!mosseSemplici.isEmpty()) {
            int[] m = mosseSemplici.get(new Random().nextInt(mosseSemplici.size()));
            applicatoAI(m);
        }
    }

    private void applicatoAI(int[] m) {
        casellaSelezionata = caselle[m[0]][m[1]];
        rSel = m[0]; cSel = m[1];
        eseguiMossa(m[2], m[3], m[4], m[5]);
    }

    private boolean isNemicoAI(int r, int c) {
        if (caselle[r][c].getChildCount() == 0) return false;
        return caselle[r][c].getChildAt(0).getTag().toString().startsWith("NERO");
    }

    private void ruotaPezzi(float rotation) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (caselle[r][c].getChildCount() > 0) {
                    caselle[r][c].getChildAt(0).animate().rotation(rotation).setDuration(600).start();
                }
            }
        }
    }

    private void aggiornaInterfacciaTurno() {
        if (turnoNeri) {
            txtStatus.setText("TURNO: NERO");
            txtStatus.setTextColor(Color.WHITE);
            rootLayout.setBackgroundColor(Color.parseColor("#212121"));
        } else {
            txtStatus.setText("TURNO: BIANCO (PC)");
            txtStatus.setTextColor(Color.BLACK);
            rootLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }
    }

    private boolean puoAncoraMangiare(int r, int c) {
        int[][] dirs = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
        for (int[] d : dirs) {
            int tr = r + d[0], tc = c + d[1];
            if (tr >= 0 && tr < 8 && tc >= 0 && tc < 8) {
                if (caselle[tr][tc].getChildCount() == 0 && isNemico(r + d[0] / 2, c + d[1] / 2)) return true;
            }
        }
        return false;
    }

    private void controllaVittoria() {
        int b = 0, n = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (caselle[r][c].getChildCount() > 0) {
                    if (caselle[r][c].getChildAt(0).getTag().toString().startsWith("BIANCO")) b++;
                    else n++;
                }
            }
        }
        if (b == 0) Toast.makeText(this, "NERI VINCONO!", Toast.LENGTH_LONG).show();
        if (n == 0) Toast.makeText(this, "BIANCHI VINCONO!", Toast.LENGTH_LONG).show();
    }

    private void resetColoriScacchiera() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 != 0) caselle[r][c].setBackgroundColor(Color.parseColor("#B58863"));
            }
        }
    }

    private boolean haMosseDisponibili(boolean neri) {
        String colore = neri ? "NERO" : "BIANCO";

        int[][] dirs = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 2}, {2, -2}, {-2, 2}, {-2, -2}
        };

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (caselle[r][c].getChildCount() > 0) {
                    ImageView pezzo = (ImageView) caselle[r][c].getChildAt(0);
                    String tag = pezzo.getTag().toString();

                    if (!tag.startsWith(colore)) continue;

                    boolean dama = tag.contains("DAMA");

                    for (int[] d : dirs) {
                        int nr = r + d[0];
                        int nc = c + d[1];

                        if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) continue;
                        if (caselle[nr][nc].getChildCount() != 0) continue;

                        // Mossa semplice
                        if (Math.abs(d[0]) == 1) {
                            if (dama || (neri && d[0] == -1) || (!neri && d[0] == 1)) {
                                return true;
                            }
                        }

                        // Mangiare
                        if (Math.abs(d[0]) == 2) {
                            int mr = r + d[0] / 2;
                            int mc = c + d[1] / 2;

                            if (caselle[mr][mc].getChildCount() > 0) {
                                String tagM = caselle[mr][mc].getChildAt(0).getTag().toString();
                                if (!tagM.startsWith(colore)) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

}