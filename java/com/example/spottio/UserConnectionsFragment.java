package com.example.spottio;

import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;

public class UserConnectionsFragment extends BaseUserListFragment {

    // Passiamo sia il titolo che la lista
    public static UserConnectionsFragment newInstance(String title, ArrayList<String> users) {
        UserConnectionsFragment fragment = new UserConnectionsFragment();
        Bundle args = new Bundle();
        args.putString("TITLE", title);
        args.putStringArrayList("USER_LIST", users);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void setupFragment() {
        etSearch.setVisibility(View.GONE); // Nasconde la barra di ricerca

        if (getArguments() != null) {
            // Imposta il titolo dinamicamente
            String title = getArguments().getString("TITLE", "");
            if (tvTitle != null) tvTitle.setText(title);

            // Carica la lista
            ArrayList<String> userList = getArguments().getStringArrayList("USER_LIST");
            if (userList != null) {
                displayedList.clear();
                displayedList.addAll(userList);
                adapter.notifyDataSetChanged();
            }
        }
    }
}