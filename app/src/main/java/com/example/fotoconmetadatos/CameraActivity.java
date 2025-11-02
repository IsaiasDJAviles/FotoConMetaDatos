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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_LOCATION = 101;

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private File photoFile;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnCapture = findViewById(R.id.btnCapture);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnCapture.setOnClickListener(v -> checkPermissionsAndTakePhoto());

        // Solicitar ubicación al inicio
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
                            tvStatus.setText("GPS listo. Lat: " + String.format("%.4f", location.getLatitude()) +
                                    ", Lon: " + String.format("%.4f", location.getLongitude()));
                        } else {
                            tvStatus.setText("Ubicación no disponible. La foto se guardará sin GPS.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvStatus.setText("Error al obtener ubicación");
                    });
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = createImageFile();

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        } else {
            Toast.makeText(this, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al crear archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            if (photoFile != null && photoFile.exists()) {
                // Agregar metadatos EXIF
                addExifData(photoFile);

                // Agregar la foto al MediaStore
                addPhotoToGallery(photoFile);

                Toast.makeText(this, "Foto guardada con éxito", Toast.LENGTH_SHORT).show();

                // Volver a MainActivity
                finish();
            }
        }
    }

    private void addExifData(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());

            // Agregar fecha y hora
            String dateTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",
                    Locale.getDefault()).format(new Date());
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateTime);
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTime);
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateTime);

            // Agregar GPS si está disponible
            if (currentLocation != null) {
                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();
                double altitude = currentLocation.getAltitude();

                exif.setLatLong(latitude, longitude);
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,
                        String.valueOf(altitude));

                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,
                        new SimpleDateFormat("yyyy:MM:dd", Locale.getDefault()).format(new Date()));
                exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,
                        new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));

                Toast.makeText(this, "GPS agregado: " + String.format("%.4f, %.4f",
                        latitude, longitude), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "GPS no disponible, foto guardada sin ubicación",
                        Toast.LENGTH_SHORT).show();
            }

            exif.saveAttributes();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al agregar metadatos: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addPhotoToGallery(File file) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());

        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Notificar al sistema que hay un nuevo archivo
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
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
