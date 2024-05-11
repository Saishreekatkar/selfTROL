package com.selftrol.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListUtils {

    public static CustomAdapter createCustomAdapter(Context context, List<ResolveInfo> resolveInfos, List<String> appNames, List<Drawable> appIcons, List<String> packageNames) {
        return new CustomAdapter(context, resolveInfos, appNames, appIcons, packageNames);
    }

    public interface OnCheckboxClickListener {
        void onCheckboxClick(String packageName, boolean isChecked);
    }

    public static class CustomAdapter extends ArrayAdapter<ResolveInfo> {
        private final Context context;
        private final List<ResolveInfo> resolveInfos;
        private final List<Boolean> checkedStates;
        private final List<String> appNames;
        private final List<Drawable> appIcons;
        private final List<String> packageNames;
        private OnCheckboxClickListener checkboxClickListener;

        public CustomAdapter(Context context, List<ResolveInfo> resolveInfos, List<String> appNames, List<Drawable> appIcons, List<String> packageNames) {
            super(context, R.layout.list_item, resolveInfos);
            this.context = context;
            this.resolveInfos = resolveInfos;
            this.checkedStates = new ArrayList<>(Collections.nCopies(resolveInfos.size(), false));
            this.appNames = appNames;
            this.appIcons = appIcons;
            this.packageNames = packageNames;
        }

        public void setOnCheckboxClickListener(OnCheckboxClickListener listener) {
            this.checkboxClickListener = listener;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder") View rowView = inflater.inflate(R.layout.list_item, parent, false);

            TextView textView = rowView.findViewById(R.id.appNameTextView);
            CheckBox checkBox = rowView.findViewById(R.id.appCheckBox);

            ResolveInfo resolveInfo = resolveInfos.get(position);
            String appName = appNames.get(position);
            String packageName = packageNames.get(position);

            textView.setText(appName);
            checkBox.setChecked(checkedStates.get(position));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkedStates.set(position, isChecked);
                if (checkboxClickListener != null) {
                    checkboxClickListener.onCheckboxClick(packageName, isChecked);
                }
            });

            return rowView;
        }
    }
}
