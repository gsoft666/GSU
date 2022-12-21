package com.gsu.utils;

/*Imports*/

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /*Para formar el 1er dialogo*/
    AlertDialog.Builder builder;

    /*Para controlar la salida de la app*/
    boolean doubleBackToExitPressedOnce = false;

    /*Booleano para registrar el broadcastReceiver*/
    boolean isRegistred = false;

    /*Check y progressbar de la pantalla*/
    CheckBox check;
    ProgressBar p;
    TextView t;
    TextView versionApp;

    /*String's para usar con los datos que vienen desde GX*/
    String nombreAPP;
    String nombrePackage;
    String urlApp;
    String nombreApk;

    //Permiso 666 del demonio!!
    private static final int PERMISO_EXTERNAL_STORAGE = 666;
    private static final int PERMISO_EXTERNAL_STORAGE_FAIL = 667;
    private static final int UNINSTALL_REQUEST_CODE = 1;

    //Carpeta de descargas a fuego
    private static final File DOWNLOAD_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APP_GSOFT");
    private static final File BACKUP_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APPS_GSOFT_RESPALDO/");
    private static final String ACTION_INSTALL_COMPLETE = "INSTALACION COMPLETA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Traigo datos del intent anterior*/
        Intent dataFromStart = getIntent();
        nombreAPP = dataFromStart.getStringExtra("nombreAPP");
        nombrePackage = dataFromStart.getStringExtra("nombrePackage");
        urlApp = dataFromStart.getStringExtra("urlApp");
        nombreApk = dataFromStart.getStringExtra("nombreApk");

        //Version pa mostrar en screen
        String versionActual = BuildConfig.VERSION_NAME;

        /*Asigno y busco por los id en pantalla*/
        check = findViewById(R.id.checkAppDesinstalada);
        p = findViewById(R.id.progressBar);
        t = findViewById(R.id.textView2);
        versionApp = findViewById(R.id.versionTxt);

        versionApp.setText(versionActual);
        /*Instancio el CP y traigo los datos*/

        /*Para poner el nombre de la app que se descarga en pantalla*/
        String textview = getString(R.string.descargando_app, nombreApk);
        t.setText(textview);

        try {
            //El permiso se requiere SOLO la 1ra vez que se instala la aplicación
            //Lo primero que checkeo es si el dispositivo tiene permisos para escribir en el External Storage (Descargas)

            /*Intent Filter*/
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INSTALL_COMPLETE);

            registerReceiver(mIntentReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            registerReceiver(mIntentReceiver, intentFilter);
            isRegistred = true;
            permisoParaGrabar(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISO_EXTERNAL_STORAGE);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*Borrar app*/
    public void borrar() {
        System.out.println(nombrePackage);
        String app_pkg_name = nombrePackage;
        int aux = 0;
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages;
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo s : packages) {
            //String aux = String.valueOf(s);
            if (s.packageName.equals(app_pkg_name)) {
                System.out.println("Existe la aplicacion!. " + s.packageName);
                aux += 1;
                break;
            }
        }
        if (aux == 1) {
            System.out.println("Desinstalando e instalando nueva version!");
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + app_pkg_name));
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(intent, UNINSTALL_REQUEST_CODE);
        } else {
            try {
                System.out.println("No se encontró la aplicación instalada, descargando");
                check.setChecked(true);
                descarga();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void borrarRecursivo(File carpeta) {
        if (carpeta.isDirectory())
            for (File archivos : carpeta.listFiles())
                if (archivos.exists()) {
                    borrarRecursivo(archivos);
                }
        carpeta.delete();
        System.out.println("Archivo borrado!");
    }

    //Otra cosa que no se que hace pero funciona, muahaha
    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }

    public void descarga() throws IOException, InterruptedException {
        final DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //url de descarga de la app
        Uri uri2 = Uri.parse(urlApp);
        p.setIndeterminate(true);
        DownloadManager.Request request = new DownloadManager.Request(uri2);
        String filename = nombreApk;
        String mimetype = "application/vnd.android.package-archive";
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(true);
        request.setMimeType(mimetype);
        request.setTitle(nombreApk);
        request.setDescription("Descargando APP " + nombreApk);

        File carpeta = DOWNLOAD_DIRECTORY;
        //Si no existe creamos la carpeta soopapp
        if (!carpeta.exists()) {
            carpeta.mkdir();
            System.out.println("Creando carpeta GSOFT_DATA");
        } else {
            System.out.println("Carpeta GSOFT_DATA ya existe");
        }

        //lo envio a la carpeta en descargas
        request.setDestinationUri(Uri.parse("file://" + carpeta + File.separator + filename));

        //Checkeo si el archivo existe, si es asi lo borro
        File archivoFinal = new File(carpeta + File.separator + filename);
        if (archivoFinal.exists()) {
            archivoFinal.delete();
            System.out.println("borrando archivo " + archivoFinal.getName());
        }
        //Descargo la app y lo registro en el Receiver
        downloadmanager.enqueue(request);
        Toast.makeText(this, "Descargando nueva versión", Toast.LENGTH_LONG).show();
        registerReceiver(mDescarga, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //Si el SDK es mayor a 23, solicito permiso para escribir en la carpeta
    public void permisoParaGrabar(String permiso, int code) throws IOException, InterruptedException {
        if (Build.VERSION.SDK_INT > 23) {
            //Si no tiene permisos para escribir en la carpeta descargas lo solicito
            if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{permiso}, code);
            } else {
                borrar();
            }

        } else { //permission is automatically granted on sdk<23 upon installation
            borrar();
        }
    }

    //Este metodo la verdad que no se que hace, se que instala el package muahahaha
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void installPackage(Context context, InputStream in, String packageName)
            throws IOException {

        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite("COSOPUM", 0, -1);
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        in.close();
        out.close();
        session.commit(createIntentSender(context, sessionId));
    }


    private final BroadcastReceiver mDescarga = new BroadcastReceiver() {
        // @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                File[] f = DOWNLOAD_DIRECTORY.listFiles();
                FileInputStream archivoIS = null;
                try {
                    archivoIS = new FileInputStream(f[0]);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    //Checkeo si el array no está vacío
                    if (!ArrayUtils.isEmpty(f)) {
                        archivoIS = new FileInputStream(f[0]);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    installPackage(context, archivoIS, "");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {

            //aca manejo el estado de la instalación
            String action = intent.getAction();
            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APP_GSOFT");
            File fBackup = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APPS_GSOFT_RESPALDO/");
            Bundle extras = intent.getExtras();

            String errores = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);

            if (ACTION_INSTALL_COMPLETE.equals(action)) {
                int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE);
                switch (result) {
                    case PackageInstaller.STATUS_PENDING_USER_ACTION:
                        startActivity((Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT));
                        break;
                    //Si se instaló correcto, reinicio la app con el nomre del packge
                    case PackageInstaller.STATUS_SUCCESS:
                        PackageManager pm = getPackageManager();
                        List<ApplicationInfo> packages;
                        packages = pm.getInstalledApplications(0);
                        for (ApplicationInfo s : packages) {
                            if (s.packageName.equals(nombrePackage)) {
                                System.out.println("Existe la aplicacion!." + s.packageName);
                                borrarRecursivo(f);
                                borrarRecursivo(fBackup);
                                Toast.makeText(context.getApplicationContext(), "Abriendo aplicación", Toast.LENGTH_LONG).show();
                                Intent launchIntent = pm.getLaunchIntentForPackage(nombrePackage);
                                finishAndRemoveTask();
                                context.startActivity(launchIntent);
                            }
                        }
                        break;
                    case PackageInstaller.STATUS_FAILURE_ABORTED:
                        //Si el usuario canceló porque se la da de pija grande, también borro
                        borrarRecursivo(f);
                        borrarRecursivo(fBackup);
                        Toast.makeText(context.getApplicationContext(), "Cancelado por el usuario, descargando otra vez..", Toast.LENGTH_LONG).show();
                        //MainActivity.this.finishAndRemoveTask();
                        try {
                            descarga();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    //En cualquier otra situacion, borro el APK
                    default:
                        borrarRecursivo(f);
                        borrarRecursivo(fBackup);
                        System.out.println("Errores: " + errores);
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*Solo si se registró previamente lo finalizo al receiver*/
        if (isRegistred) {
            System.out.println("Estoy en el destroy!");
            unregisterReceiver(mIntentReceiver);
            unregisterReceiver(mDescarga);
            isRegistred = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Pulsa de nuevo para salir", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UNINSTALL_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Toast.makeText(this, "aca estoy ahora", Toast.LENGTH_LONG).show();
                    /*Checkeo en true en pantalla*/
                    check.setChecked(true);
                    try {
                        descarga();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case RESULT_FIRST_USER:
                    // Toast.makeText(this, "aca estoy", Toast.LENGTH_LONG).show();
                    try {
                        descarga();
                    } catch (IOException | InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case RESULT_CANCELED:
                    borrar();
                    break;

                default:
                    Toast.makeText(this, requestCode, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISO_EXTERNAL_STORAGE:
                //Si el usuario da permiso, descargo e instalo
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    borrar();
                }
            case PERMISO_EXTERNAL_STORAGE_FAIL:
                //Si le pone que no porque se la da de pija, salgo y entro otra vez a la app
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Debe aceptar los permisos para actualizar la aplicacion", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }
}