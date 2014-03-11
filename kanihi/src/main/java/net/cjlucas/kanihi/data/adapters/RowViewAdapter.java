package net.cjlucas.kanihi.data.adapters;

import android.view.View;
import android.view.ViewGroup;

public interface RowViewAdapter<E> {
    View getRowView(E model, View view, ViewGroup viewGroup);
}
