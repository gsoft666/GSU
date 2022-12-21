package com.gsu.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class InstalarBackup extends AppCompatActivity {

    private static final File BACKUP_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "APPS_GSOFT_RESPALDO/");
    private static final String ACTION_INSTALL_COMPLETE = "INSTALACION COMPLETA";

    private static final int PERMISO_EXTERNAL_STORAGE = 666;
    private static final int PERMISO_EXTERNAL_STORAGE_FAIL = 667;

    //Lista que se muestra en pantalla
    ListView lista;

    //Packagemanager y packageInfo es para conseguir la info de las aplicaciones, el icono y el nombre de la app
    PackageManager pm;
    PackageInfo pi;

    String appName;
    Drawable apkIcon;
    String rutaArchivo;
    String nombrePackage;

    //este array de archivos lo asignamos a BACKUP_DIRECTORY
    File[] archivos;
    File fBackup;
    //Lista array de la clase APK, para instanciar y asignar a la lista
    ArrayList<Apk> listaArchivos;
    //Apk adapter para transformar los datos a la lista final
    ApkAdapter apkAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instalar_backup);
        lista = findViewById(R.id.listView);

        try {
            permisoParaGrabar(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISO_EXTERNAL_STORAGE);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void metodoMain() {

        archivos = BACKUP_DIRECTORY.listFiles();
        fBackup = BACKUP_DIRECTORY;
        pm = getPackageManager();
        listaArchivos = new ArrayList<>();

        for (File f : archivos) {

            rutaArchivo = f.getAbsolutePath();
            pi = pm.getPackageArchiveInfo(f.getPath(), 0);
            pi.applicationInfo.sourceDir = f.getPath();
            pi.applicationInfo.publicSourceDir = f.getPath();
            /*El icono*/
            apkIcon = pi.applicationInfo.loadIcon(pm);
            /*Nombre de la aplicacion*/
            appName = (String) pi.applicationInfo.loadLabel(pm);
            /*Nombre del package*/
            nombrePackage = pi.applicationInfo.packageName;
            //Lo agrego a la una lista y hago un new del apk
            listaArchivos.add(new Apk(apkIcon, appName, rutaArchivo, nombrePackage));
        }
        apkAdapter = new ApkAdapter(this, R.layout.lista_archivos, listaArchivos);
        lista.setAdapter(apkAdapter);

        lista.setOnItemClickListener((adapterView, view, position, id) -> {

            /*Registramos un intentFilter para controlar la instalación*/
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INSTALL_COMPLETE);
            registerReceiver(mIntentReceiver, intentFilter);

            Apk claseApk = (Apk) adapterView.getItemAtPosition(position);
            //Obtenemos la ruta guardada con el getter
            String file = claseApk.getRutaArchivo();
            nombrePackage = claseApk.getPackageName();

            //Convertimos a file la ruta del archivo
            File fa = new File(file);
            FileInputStream archivoIS;
            //Lo convertimos a FileInputStream
            try {
                archivoIS = new FileInputStream(fa);
                installPackage(getApplicationContext(), archivoIS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void permisoParaGrabar(String permiso, int code) throws IOException, InterruptedException {
        if (Build.VERSION.SDK_INT > 23) {
            //Si no tiene permisos para escribir en la carpeta descargas lo solicito
            if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{permiso}, code);
            } else {
                metodoMain();
            }

        } else { //permission is automatically granted on sdk<23 upon installation
            metodoMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISO_EXTERNAL_STORAGE:
                //Si el usuario da permiso, descargo e instalo
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    metodoMain();
                }
            case PERMISO_EXTERNAL_STORAGE_FAIL:
                //Si le pone que no porque se la da de pija, salgo y entro otra vez a la app
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Debe aceptar los permisos para utilizar la aplicación", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    public static void installPackage(Context context, InputStream in)
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

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //aca manejo el estado de la instalación
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            String errores = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);

            if (ACTION_INSTALL_COMPLETE.equals(action)) {
                int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE);
                switch (result) {
                    case PackageInstaller.STATUS_PENDING_USER_ACTION:
                        startActivity((Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT));
                        break;
                    //Si se instaló correcto, inicio la app instalada
                    case PackageInstaller.STATUS_SUCCESS:
                        PackageManager pm = getPackageManager();
                        List<ApplicationInfo> packages;
                        packages = pm.getInstalledApplications(0);
                        for (ApplicationInfo s : packages) {
                            if (s.packageName.equals(nombrePackage)) {
                                borrarRecursivo(fBackup);
                                Toast.makeText(context.getApplicationContext(), "Abriendo aplicación", Toast.LENGTH_LONG).show();
                                Intent launchIntent = pm.getLaunchIntentForPackage(nombrePackage);
                                finishAndRemoveTask();
                                context.startActivity(launchIntent);
                            }
                        }
                        break;
                    case PackageInstaller.STATUS_FAILURE_ABORTED:
                        Toast.makeText(context.getApplicationContext(), "Cancelando", Toast.LENGTH_LONG).show();
                        break;
                    //Muestro errores cualquier si hay cualquier error en los errores
                    default:
                        Toast.makeText(context.getApplicationContext(), "Errores: " + errores, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };

    public void borrarRecursivo(File carpeta) {
        if (carpeta.isDirectory())
            for (File archivos : carpeta.listFiles())
                if (archivos.exists()) {
                    borrarRecursivo(archivos);
                }
        carpeta.delete();
        System.out.println("Archivo borrado!");
    }
}