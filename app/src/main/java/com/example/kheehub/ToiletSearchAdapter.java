package com.example.kheehub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ToiletSearchAdapter extends ArrayAdapter<Toilet> implements Filterable {
    private final List<Toilet> allToilets;
    private List<Toilet> filtered;

    public ToiletSearchAdapter(Context context, List<Toilet> toilets) {
        super(context, android.R.layout.simple_dropdown_item_1line, toilets);
        this.allToilets = new ArrayList<>(toilets);
        this.filtered = new ArrayList<>(toilets);
    }

    @Override
    public int getCount() { return filtered.size(); }

    @Override
    public Toilet getItem(int position) { return filtered.get(position); }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        Toilet t = filtered.get(position);
        ((TextView) convertView).setText(t.getName() + " — " + t.getFloor());
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Toilet> suggestions = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    suggestions.addAll(allToilets);
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    for (Toilet t : allToilets) {
                        if (t.getName().toLowerCase().contains(query)
                                || t.getFloor().toLowerCase().contains(query)) {
                            suggestions.add(t);
                        }
                    }
                }
                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered = (List<Toilet>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}