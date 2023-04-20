package com.example.filetransfer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.namespace.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class localFileAdapter extends ArrayAdapter<FileItem> {
    int iLayout;
    private Context context;


    public localFileAdapter(Context context,int resource, List<FileItem> objects) {
      super(context, resource, objects);
        iLayout = resource;
        this.context = context;

    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public View getView(final int position,  View convertView, ViewGroup parent) {
        FileItem item = getItem(position);
        View v;
        ViewHolder holder;
        long size;

        if(convertView == null)
        {
            v =  LayoutInflater.from(getContext()).inflate(iLayout,null);
            holder = new ViewHolder();
            holder.checkbox =  v.findViewById(R.id.item_checkbox);
            holder.folder = v.findViewById(R.id.item_image);
            holder.filename = v.findViewById(R.id.item_filename);
            holder.filesize = v.findViewById(R.id.item_filesize);
            v.setTag(holder);
        }
        else{
            v = convertView;
            holder = (ViewHolder) v.getTag();
        }

        if(item.getChecked())
        {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(true);
        }
       else
        {
            holder.checkbox.setVisibility(View.GONE);
            holder.checkbox.setChecked(false);
        }

   /*     if(item.isCheckBox1())
            holder.checkbox.setChecked(true);
        else
            holder.checkbox.setChecked(false);
        holder.checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b = Objects.requireNonNull(getItem(position)).isCheckBox1();
                Objects.requireNonNull(getItem(position)).setCheckBox1(!b);
            }
        });
        */

       if(item.getFile().isDirectory())   // folder
       {
           holder.folder.setImageBitmap(BitmapFactory.decodeResource(context.getResources(),R.mipmap.logo1));
           holder.filesize.setText("--");
       }
       else
       {
           size = item.getFile().length();
           holder.folder.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.mipmap.list1));
           holder.filesize.setText(String.format("%6.2f " + getunit(size),getsize(size)));
       }
       holder.filename.setText(item.getFile().getName());
       return v;
    }
    public class ViewHolder{
        CheckBox checkbox;
        ImageView folder;
        TextView filename;
        TextView filesize;
    }

    private float getsize(long size ){
        if(size < 1024)
            return size;
        else
        {
            if(size < 1024 * 1024)
                 return (float)size / 1024;
            else
                return (float)size / (1024 * 1024);
        }
    }
    private String getunit(long size ){
        if(size < 1024)
            return "b";
        else
        {
            if(size < 1024 * 1024)
                return "kb";
            else
                return "mb";
        }
    }

    private Bitmap getVideoattr(String fileName) {
        try{
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileName);
            return retriever.getFrameAtTime();
        }catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }


    }
    private Bitmap getabbr(String fileName) {
         BitmapFactory.Options option = new BitmapFactory.Options();
         option.inJustDecodeBounds = false;
         option.inSampleSize = 4;
         return BitmapFactory.decodeFile(fileName,option);
    }


}
