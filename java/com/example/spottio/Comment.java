package com.example.spottio;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class Comment implements Serializable {

    // Diciamo a Firebase che in Java si chiama "author", ma nel database si chiama "user"
    @PropertyName("user")
    private String author;

    private String text;

    // RISOLUZIONE CRASH: Ora è una Date nativa compatibile con Firebase
    private Date timestamp;

    // --- 1. COSTRUTTORE VUOTO OBBLIGATORIO ---
    public Comment() {
    }

    // --- 2. COSTRUTTORE COMPLETO ---
    public Comment(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date(); // Salva il momento esatto della creazione
    }

    // --- 3. GETTER E SETTER ---
    @PropertyName("user")
    public String getAuthor() { return author; }

    @PropertyName("user")
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    // --- 4. FORMATTAZIONE DATA ---
    // @Exclude dice a Firebase di non cercare questo dato nel database, perché serve solo per la grafica
    @Exclude
    public String getFormattedDate() {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        return sdf.format(timestamp);
    }
}