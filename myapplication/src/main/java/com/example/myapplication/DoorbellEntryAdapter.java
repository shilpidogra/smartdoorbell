package com.example.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.things.pio.Gpio;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;



/**
 * Created by Ayushi on 23/3/18.
 */

/**
 * RecyclerView adapter to populate doorbell entries from Firebase.
 */

public class DoorbellEntryAdapter extends
        FirebaseRecyclerAdapter<DoorbellEntry, DoorbellEntryAdapter.DoorbellEntryViewHolder> {


    /**
     * ViewHolder for each doorbell entry
     */

    public static class DoorbellEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView time;
        public final TextView metadata;
        public final Switch switchRed;

        public DoorbellEntryViewHolder(View itemView) {
            super(itemView);

            this.image = (ImageView) itemView.findViewById(R.id.imageView1);
            this.time = (TextView) itemView.findViewById(R.id.textView1);
            this.metadata = (TextView) itemView.findViewById(R.id.textView2);
            this.switchRed = itemView.findViewById(R.id.switch1);
        }
    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;
    public Gpio redIO;
    public final String TAG = "Adapter";

    public DoorbellEntryAdapter(TabFragment1 context, DatabaseReference ref) {
        super(new FirebaseRecyclerOptions.Builder<DoorbellEntry>()
                .setQuery(ref, DoorbellEntry.class)
                .build());

        mApplicationContext = context.getContext();
        mFirebaseStorage = FirebaseStorage.getInstance();

    }

    @Override
    public DoorbellEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout, parent, false);

        return new DoorbellEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(DoorbellEntryViewHolder holder, int position, DoorbellEntry model) {

        // Display the timestamp

        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.time.setText(prettyTime);

        // Display the image

        if (model.getImage() != null) {
            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.ic_cloud_download_black_24dp);

            Glide.with(mApplicationContext)
                    .load(imageRef)
                    .apply(requestOptions)
                    .into(holder.image);
        }

        // Display the metadata

        if (model.getAnnotations() != null) {
            ArrayList<String> keywords = new ArrayList<>(model.getAnnotations().keySet());

            int limit = Math.min(keywords.size(), 3);
            holder.metadata.setText(TextUtils.join("\n", keywords.subList(0, limit)));
        } else {
            holder.metadata.setText("no annotations yet");
        }



        holder.switchRed.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 final boolean isChecked) {
                        Toast.makeText(mApplicationContext, "Yet to be implemented", Toast.LENGTH_LONG).show();
                    }
                });

    }
}