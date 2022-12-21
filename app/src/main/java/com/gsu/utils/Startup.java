package com.gsu.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Startup extends AppCompatActivity {

    private static final File BACKUP_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APPS_GSOFT_RESPALDO/");
    private static final int PERMISO_EXTERNAL_STORAGE = 666;
    private static final int PERMISO_EXTERNAL_STORAGE_FAIL = 667;

    /*String's para usar con los datos que vienen desde GX*/
    String nombreAPP;
    String nombrePackage;
    String urlApp;
    String nombreApk;

    File[] f;

    //Content provider - De esta forma comunicamos con aplicaciones externas
    /*En las app de GX hay un Content provider expuesto, con un SQL lite que persiste los datos*/
    private static final Uri CP = Uri.parse("content://gsoft.cp.data/data");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        f = BACKUP_DIRECTORY.listFiles();
        /*Instancio el CP y traigo los datos*/
        String dataRaw = getDataCP();
        /*Split por ;*/
        String[] datos = dataRaw.split(";");

        System.out.println("Datossss " + dataRaw);
        /*Checkeo que estèn los datos, sino los cargo a fuego por el momento*/
        if (datos.length == 4) {
            System.out.println("Usando datos del CP!");
            nombreAPP = datos[0];
            nombrePackage = datos[1];
            urlApp = datos[2];
            nombreApk = datos[3];
            setContentView(R.layout.activity_startup);

            TextView versiontxt = (TextView) findViewById(R.id.versionTxtMain);
            String versionActual = BuildConfig.VERSION_NAME;
            versiontxt.setText(versionActual);

            try {
                permisoParaGrabar(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISO_EXTERNAL_STORAGE);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            //Si hay datos en la carpeta de backup y los datos del CP están en 0 quiere decir que el usuario abrió a mano GSU porque seguramente la cagó actualizando
        } else if ( f != null && f.length > 0 && datos.length == 1) {
            //Si en la carpeta de backup hay un archivo abro la pantalla de instalación de emelgencia
            Intent aLaGrandeLePusecuca = new Intent(this, InstalarBackup.class);
            startForResult.launch(aLaGrandeLePusecuca);

        } else {
            Intent adminMenu = new Intent(this, AdminMenu.class);
            startForResult.launch(adminMenu);
        }
    }

    /*Muestra el menu en la barra de la appsdg*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public String getDataCP() {
        ContentResolver CR = getContentResolver();
        String[] valores_recuperar = {"_id", "parm"};
        Cursor c = CR.query(CP, valores_recuperar, null, null, null);
        int id;
        String parm = "";
        if (c != null && c.moveToFirst()) {
            c.moveToFirst();
            do {
                id = c.getInt(0);
                parm = c.getString(1);
                System.out.println("QUERY SQL_LITE: " + id + ", " + parm + ".");

            } while (c.moveToNext());
            c.close();
        }
        return parm;
    }


    public void Aceptar(View view) {
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("nombreAPP", nombreAPP);
        mainActivity.putExtra("nombrePackage", nombrePackage);
        mainActivity.putExtra("urlApp", urlApp);
        mainActivity.putExtra("nombreApk", nombreApk);
        Startup.this.startActivity(mainActivity);
    }


    public void backupApp() {

        File fileDestino = new File(BACKUP_DIRECTORY + File.separator + nombreAPP + ".apk");

        //Si el archivo existe lo borro
        if (fileDestino.exists()) {
            fileDestino.delete();
            System.out.println("borrando archivo " + fileDestino.getName());
        }


        File carpeta = BACKUP_DIRECTORY;
        //Si no existe creamos la carpeta soopapp
        if (!carpeta.exists()) {
            carpeta.mkdirs();
            System.out.println("Creando carpeta APPS_GSOFT_RESPALDO");
        } else {
            System.out.println("Carpeta APPS_GSOFT_RESPALDO ya existe");
        }


        int flags = PackageManager.GET_META_DATA |
                PackageManager.GET_SHARED_LIBRARY_FILES;
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> applications = pm.getInstalledApplications(flags);
        //Recorro las aplicaciones instaladas
        for (ApplicationInfo appInfo : applications) {
            //Estas son las aplicaciones del sistema
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {

            } else {
                /*
                 * Estas son las isntaladas por los usuarios
                 * Si encuentra la aplicacion instalada la intento backapear
                 * */
                if (appInfo.packageName.equals(nombrePackage)) {
                    //Creo archivos origen y destino
                    File fileOrigen = new File(appInfo.publicSourceDir);
                    try {
                        //COpio el archivo al destino
                        copyApk(new File(fileOrigen.getAbsolutePath()), fileDestino);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void copyApk(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public void permisoParaGrabar(String permiso, int code) throws IOException, InterruptedException {
        //Si no tiene permisos para escribir en la carpeta descargas lo solicito
        if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permiso}, code);
        } else {
            backupApp();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISO_EXTERNAL_STORAGE:
                //Si el usuario da permiso, descargo e instalo
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backupApp();
                }

            case PERMISO_EXTERNAL_STORAGE_FAIL:
                //Si le pone que no porque se la da de pija, salgo y entro otra vez a la app
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Debe aceptar los permisos para actualizar la aplicacion", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    /*Manejo el resultado */
    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == Activity.RESULT_OK) {
            Toast.makeText(this, "Cojo si", Toast.LENGTH_LONG).show();
        }

        if (result.getResultCode() == 66) {
            finishAndRemoveTask();
        }

    });

    @Override
    public void onBackPressed() {

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {


        if (item.getItemId() == R.id.menuConf) {
            Intent adminMenu = new Intent(this, AdminMenu.class);
            startForResult.launch(adminMenu);
        }

        return super.onOptionsItemSelected(item);
    }
}