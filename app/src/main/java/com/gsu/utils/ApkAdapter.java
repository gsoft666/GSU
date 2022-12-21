package com.gsu.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ApkAdapter extends ArrayAdapter<Apk> {

    private final Context mcontext;
    private final int mResource;

    public ApkAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Apk> lista) {
        super(context, resource, lista);
        this.mcontext = context;
        this.mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layout = LayoutInflater.from(mcontext);

        convertView = layout.inflate(mResource, parent, false);
        ImageView img = convertView.findViewById(R.id.imagenList);
        TextView txtNombre = convertView.findViewById(R.id.textoList);
        img.setImageDrawable(getItem(position).getIcono());
        txtNombre.setText(getItem(position).getNombreApp());
        return convertView;
    }
}
