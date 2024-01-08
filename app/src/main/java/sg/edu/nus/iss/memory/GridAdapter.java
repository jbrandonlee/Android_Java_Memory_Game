package sg.edu.nus.iss.memory;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

public class GridAdapter extends ArrayAdapter<Object> {
    private final Context context;
    protected List<File> images;

    public GridAdapter(Context ctx, List<File> images) {
        super(ctx, R.layout.grid_cell);
        this.context = ctx;
        this.images = images;
        addAll(new Object[images.size()]);
    }

    public void updateImages(List<File> images) {
        this.images = images;
        clear();
        addAll(new Object[images.size()]);   // Specify total no. of cells in grid
    }

    public View getView(int pos, View view, @NonNull ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.grid_cell, parent, false);
        }

        // Set Image
        ImageView imageView = view.findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(images.get(pos).getAbsolutePath());
        imageView.setImageBitmap(bitmap);
        return view;
    }
}
