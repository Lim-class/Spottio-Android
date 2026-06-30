package com.example.spottio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class GroupAdapter extends ArrayAdapter<GroupInfo> {

    public GroupAdapter(Context context, List<GroupInfo> groups) {
        super(context, 0, groups);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        GroupInfo group = getItem(position);
        TextView text = convertView.findViewById(android.R.id.text1);

        if (group != null && text != null) {
            text.setText(group.getName());
            text.setTextSize(18);
            text.setPadding(24, 32, 24, 32);
        }

        return convertView;
    }
}