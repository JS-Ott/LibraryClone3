package jso.libcat3.form;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import jso.libcat3.R;
import jso.libcat3.db.DbAdapter;
import jso.libcat3.db.ThreeRingBinder;
import jso.libcat3.display.DisplaySuperclass;

public class SearchForm extends ListActivity {
    private DbAdapter dbAdapter;
    private CheckBox booksBox;
    private CheckBox storyBox;
    private SearchView searchView;
    private String queryString = "";

    private static final String[] from = new String[] {
            DbAdapter.KEY_TITLE,
            DbAdapter.KEY_SORTBY,
            DbAdapter.KEY_FORMATEBOOK,
            DbAdapter.KEY_FORMATHARDCOPY
    };
    private static final int[] to = new int[] {
            R.id.tv_title,
            R.id.tv_author,
            R.id.img_left,
            R.id.img_right
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();

        dbAdapter = new DbAdapter(this);
        dbAdapter.open();

        setContentView(R.layout.layout_search);

        booksBox = (CheckBox) findViewById(R.id.search_cbox_book);
        booksBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                performSearch(queryString);
            }
        });

        storyBox = (CheckBox) findViewById(R.id.search_cbox_story);
        storyBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                performSearch(queryString);
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_form, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            queryString = intent.getStringExtra(SearchManager.QUERY);
            performSearch(queryString);
        }
    }

    private void performSearch(String searchString) {
        if (EntryForm.isBlank(searchString)) {
            Toast.makeText(SearchForm.this,
                    "Search string may not be empty",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean books = booksBox.isChecked();
        boolean story = storyBox.isChecked();

        Cursor results = dbAdapter.searchCatalog(searchString, books, story);

        fillResults(results);
    }

    @SuppressWarnings("deprecation")
    private void fillResults(Cursor results) {
        TextView tv = (TextView) findViewById(R.id.search_empty);

        if (results.getCount() == 0) {
            tv.setText(R.string.empty);
            setListAdapter(null);
        } else {
            tv.setVisibility(View.GONE);

            SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(this,
                    R.layout.row_item, results, from, to);
            cAdapter.setViewBinder(new ThreeRingBinder());
            setListAdapter(cAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dbAdapter.close();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ListAdapter arr = l.getAdapter();
        Cursor fakeItem = (Cursor) arr.getItem(position);

        long rowId = fakeItem.getLong(fakeItem.getColumnIndex(DbAdapter.KEY_LIBRARYID));

        Intent back = new Intent();
        back.putExtra(DisplaySuperclass.EXTRA_ID, rowId);

        setResult(RESULT_OK, back);
        finish();
    }
}
