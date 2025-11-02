package com.example.fotoconmetadatos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<PhotoItem> photoList;

    public PhotoAdapter(List<PhotoItem> photoList) {
        this.photoList = photoList;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoItem photo = photoList.get(position);

        // Cargar imagen con thumbnail
        Bitmap bitmap = loadThumbnail(photo.getPath());
        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap);
        }

        // Mostrar información
        String info = photo.getName() + "\n";
        if (photo.getDateTime() != null && !photo.getDateTime().isEmpty()) {
            info += "Fecha: " + photo.getDateTime() + "\n";
        }
        if (photo.getLocation() != null && !photo.getLocation().isEmpty()) {
            info += "Ubicación: " + photo.getLocation();
        }
        holder.tvInfo.setText(info);
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    private Bitmap loadThumbnail(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // Reducir tamaño para eficiencia
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateData(List<PhotoItem> newPhotoList) {
        this.photoList = newPhotoList;
        notifyDataSetChanged();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvInfo;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tvInfo = itemView.findViewById(R.id.tvInfo);
        }
    }
}