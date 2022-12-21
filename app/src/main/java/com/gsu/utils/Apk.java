package com.gsu.utils;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class Apk {
    Drawable icono;
    String nombreApp;
    String rutaArchivo;
    String packageName;


    public Apk(Drawable icono, String nombreApp, String rutaArchivo, String packageName) {
        this.icono = icono;
        this.nombreApp = nombreApp;
        this.rutaArchivo = rutaArchivo;
        this.packageName = packageName;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }


    public Drawable getIcono() {

        return icono;
    }

    public void setIcono(Drawable icono) {
        this.icono = icono;
    }

    public String getNombreApp() {
        return nombreApp;
    }

    public void setNombreApp(String nombreApp) {
        this.nombreApp = nombreApp;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
