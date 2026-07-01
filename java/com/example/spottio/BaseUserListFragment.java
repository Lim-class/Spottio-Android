package com.example.spottio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public abstract class BaseUserListFragment extends Fragment {

    protected UserAdapter adapter;
    protected ArrayList<String> displayedList = new ArrayList<>();
    protected ListView listView;
    protected EditText etSearch;
    protected TextView tvTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        listView = view.findViewById(R.id.lvUsers);
        etSearch = view.findViewById(R.id.etSearchUser);
        tvTitle = view.findViewById(R.id.tvListTitle);

        adapter = new UserAdapter(getContext(), displayedList);

        // NUOVO: Deleghiamo il click all'interfaccia pulita dell'Adapter
        adapter.setOnUserClickListener(selectedUid -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment.newInstance(selectedUid))
                    .addToBackStack(null)
                    .commit();
        });

        listView.setAdapter(adapter);
        setupFragment();

        return view;
    }

    protected abstract void setupFragment();
}