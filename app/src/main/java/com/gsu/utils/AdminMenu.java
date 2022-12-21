package com.gsu.utils;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminMenu extends AppCompatActivity {

    TextView psd;
    AlertDialog.Builder builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);
        psd = findViewById(R.id.txtPsd);

        builder = new AlertDialog.Builder(this);

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void Aceptar(View view) {

        String pass = psd.getText().toString();

        if (pass.trim().equals("e6c1bd145f")) {
            Toast.makeText(this, "SII!!", Toast.LENGTH_LONG).show();
        } else {
            mostrarDialogo();
        }
    }

    public void mostrarDialogo() {
        /*Dialog*/
        builder = new AlertDialog.Builder(this);
        builder.setMessage("Contraseña incorrecta")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, id) -> {
                    dialog.cancel();
                });

        //Crea cuadro de dialogo
        AlertDialog alert = builder.create();
        alert.setTitle("Error");
        alert.show();
    }


}