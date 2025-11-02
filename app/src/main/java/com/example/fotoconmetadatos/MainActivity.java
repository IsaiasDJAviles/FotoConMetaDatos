package com.example.fotoconmetadatos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 200;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private List<PhotoItem> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate iniciado");

        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Layout cargado");

            recyclerView = findViewById(R.id.recyclerView);
            Button btnTakePhoto = findViewById(R.id.btnTakePhoto);

            if (recyclerView == null || btnTakePhoto == null) {
                Toast.makeText(this, "Error: Views no encontradas", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error: recyclerView o btnTakePhoto es null");
                return;
            }

            // Configurar RecyclerView
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            photoList = new ArrayList<>();
            photoAdapter = new PhotoAdapter(photoList);
            recyclerView.setAdapter(photoAdapter);
            Log.d(TAG, "RecyclerView configurado");

            // Botón para tomar foto
            btnTakePhoto.setOnClickListener(v -> {
                Log.d(TAG, "Botón tomar foto presionado");
                try {
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error al abrir CameraActivity", e);
                    Toast.makeText(MainActivity.this,
                            "Error al abrir cámara: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            // Verificar y solicitar permisos
            checkPermissions();
            Log.d(TAG, "onCreate completado");

        } catch (Exception e) {
            Log.e(TAG, "Error FATAL en onCreate", e);
            Toast.makeText(this, "Error crítico: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        try {
            if (hasPermissions()) {
                loadPhotos();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onResume", e);
        }
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            Log.d(TAG, "Solicitando permisos");
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
        } else {
            Log.d(TAG, "Permisos ya concedidos");
            loadPhotos();
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void loadPhotos() {
        Log.d(TAG, "loadPhotos iniciado");
        try {
            photoList.clear();

            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
            };

            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder);

            if (cursor != null) {
                Log.d(TAG, "Cursor tiene " + cursor.getCount() + " fotos");
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String path = cursor.getString(dataColumn);
                    String name = cursor.getString(nameColumn);

                    String dateTime = "";
                    String location = "";

                    try {
                        ExifInterface exif = new ExifInterface(path);

                        dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
                        if (dateTime == null) dateTime = "";

                        double[] latLong = exif.getLatLong();
                        if (latLong != null) {
                            location = String.format("%.4f, %.4f", latLong[0], latLong[1]);
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Error leyendo EXIF de " + name);
                    }

                    photoList.add(new PhotoItem(id, path, name, dateTime, location));
                }

                cursor.close();
            } else {
                Log.w(TAG, "Cursor es null");
            }

            photoAdapter.updateData(photoList);

            if (photoList.isEmpty()) {
                Toast.makeText(this, "No hay fotos disponibles", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Cargadas " + photoList.size() + " fotos");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error FATAL al cargar fotos", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "Permisos concedidos");
                loadPhotos();
            } else {
                Log.w(TAG, "Permisos denegados");
                Toast.makeText(this, "Permisos necesarios no concedidos",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}