package com.example.fotoconmetadatos;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.File;               // Para crear archivos locales
import java.io.FileInputStream;    // Para leer archivos locales
import java.io.FileOutputStream;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_LOCATION = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private TextView tvStatus;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnCapture = findViewById(R.id.btnCapture);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Lanzador moderno para la cámara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                        addExifData(currentPhotoUri);
                        addPhotoToGallery(currentPhotoUri);
                        Toast.makeText(this, "Foto guardada con éxito", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        btnCapture.setOnClickListener(v -> checkPermissionsAndTakePhoto());

        // Solicitar ubicación
        requestLocationUpdate();
    }

    private void checkPermissionsAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            takePhoto();
        }
    }

    private void requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            tvStatus.setText("GPS listo. Lat: " +
                                    String.format("%.4f", location.getLatitude()) +
                                    ", Lon: " +
                                    String.format("%.4f", location.getLongitude()));
                        } else {
                            tvStatus.setText("Ubicación no disponible. La foto se guardará sin GPS.");
                        }
                    })
                    .addOnFailureListener(e -> tvStatus.setText("Error al obtener ubicación"));
        }
    }

    private void takePhoto() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FotoConMetaDatos");

        currentPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (currentPhotoUri != null) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No se pudo crear archivo en la galería", Toast.LENGTH_SHORT).show();
        }
    }

    private void addExifData(Uri photoUri) {
        try {
            // Copiar el contenido del Uri a un archivo temporal
            File tempFile = new File(getCacheDir(), "temp.jpg");
            try (InputStream in = getContentResolver().openInputStream(photoUri);
                 OutputStream out = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Modificar metadatos usando ExifInterface(File)
            ExifInterface exif = new ExifInterface(tempFile);
            String dateTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(new Date());
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateTime);
            if (currentLocation != null) {
                exif.setLatLong(currentLocation.getLatitude(), currentLocation.getLongitude());
            }
            exif.saveAttributes();

            // Copiar de nuevo al Uri final
            try (InputStream in = new java.io.FileInputStream(tempFile);
                 OutputStream out = getContentResolver().openOutputStream(photoUri, "w")) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            tempFile.delete(); // borrar temporal
            Toast.makeText(this, "Metadatos agregados correctamente", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al agregar metadatos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }




    private void addPhotoToGallery(Uri photoUri) {
        if (photoUri != null) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(photoUri);
            sendBroadcast(mediaScanIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                tvStatus.setText("Permiso de ubicación denegado. Las fotos no tendrán GPS.");
            }
        }
    }
}
