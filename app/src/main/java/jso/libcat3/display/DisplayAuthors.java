package jso.libcat3.display;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import jso.libcat3.R;
import jso.libcat3.db.DbAdapter;
import jso.libcat3.db.ThreeRingBinder;

public class DisplayAuthors extends DisplaySuperclass {
    protected static final String[] from = new String[] {
            DbAdapter.KEY_ID,
            DbAdapter.KEY_SORTBY,
            DbAdapter.KEY_COUNT
    };
    protected static final int[] to = new int[] {
            R.id.tv_id,
            R.id.tv_name,
            R.id.tv_count
    };

    protected TextView anthologyText;
    protected TextView storyText;
    protected TextView showAllText;


    @Override
    protected void onResume() {
        super.onResume();

        fillData();
    }

    @Override
    protected void auxCreate() {
        authorName = null;

        Intent i = getIntent();
        if (i.hasExtra(EXTRA_TYPE)) authorType = i.getIntExtra(EXTRA_TYPE, -1);
        else authorType = DbAdapter.TypeBook;

        if (authorType != DbAdapter.TypeStory) authorType = DbAdapter.TypeBook; // default to book

        // set up header views
        ListView lv = getListView();
        LayoutInflater inflater = getLayoutInflater();

        LinearLayout anthologyHeader = (LinearLayout) inflater.inflate(R.layout.auxiliary_header, lv, false);
        LinearLayout storyHeader = (LinearLayout) inflater.inflate(R.layout.auxiliary_header, lv, false);
        LinearLayout showAllHeader = (LinearLayout) inflater.inflate(R.layout.auxiliary_header, lv, false);

        showAllText = (TextView) showAllHeader.findViewById(R.id.aux_header);
        showAllText.setText("Show all books");
        showAllText.setTypeface(null, Typeface.ITALIC);
        showAllText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(DisplayAuthors.this, DisplayItems.class);
                i.putExtra(EXTRA_TYPE, DbAdapter.TypeBook);
                startActivityForResult(i, REQUEST_ITEMS);
            }
        });
        lv.addHeaderView(showAllHeader, null, false);

        anthologyText = (TextView) anthologyHeader.findViewById(R.id.aux_header);
        anthologyText.setText("Anthologies (" + dbAdapter.countAnthologies() + ")");
        anthologyText.setTypeface(null, Typeface.ITALIC);
        anthologyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DisplayAuthors.this, DisplayItems.class);
                i.putExtra(EXTRA_TYPE, DbAdapter.TypeAnthology);
                startActivityForResult(i, REQUEST_ITEMS);
            }
        });
        lv.addHeaderView(anthologyHeader, null, false);

        storyText = (TextView) storyHeader.findViewById(R.id.aux_header);
        storyText.setText("Short stories (" + dbAdapter.countStories() + ")");
        storyText.setTypeface(null, Typeface.ITALIC);
        storyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DisplayAuthors.this, DisplayAuthors.class);
                i.putExtra(EXTRA_TYPE, DbAdapter.TypeStory);
                startActivityForResult(i, REQUEST_AUTHORS);
            }
        });
        lv.addHeaderView(storyHeader, null, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void fillData() {
        int numBooks = dbAdapter.countBooks();
        setTitle(getResources().getText(R.string.app_name) + " (" + numBooks + " books)");

        anthologyText.setText("Anthologies (" + dbAdapter.countAnthologies() + ")");

        storyText.setText("Short stories (" + dbAdapter.countStories() + ")");

        Cursor authCursor;

        drawLibraryHeaders();

        if (authorType == DbAdapter.TypeStory) {
            authCursor = dbAdapter.getStoryAuthorsWithCount();
        } else {
            authCursor = dbAdapter.getBookAuthorsWithCount();
        }

        startManagingCursor(authCursor);
        SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(this, R.layout.row_author, authCursor, from, to);
        cAdapter.setViewBinder(new ThreeRingBinder());

        if (getListAdapter() == null) {
            setListAdapter(cAdapter);
        } else {
            ((SimpleCursorAdapter) getListAdapter()).swapCursor(authCursor);//.changeCursor(authCursor);
            ((SimpleCursorAdapter) getListAdapter()).notifyDataSetChanged();

            if (authCursor.getCount() == 0) {
                setListAdapter(null);
            }
        }
    }

    protected void drawLibraryHeaders() {
        showAllText.setVisibility(View.GONE);

        storyText.setVisibility(View.VISIBLE);

        if (authorType == DbAdapter.TypeStory) {
            anthologyText.setVisibility(View.GONE);
            storyText.setVisibility(View.GONE);
        }
    }

    protected void drawSlushpileHeaders() {
        storyText.setVisibility(View.GONE);

        showAllText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ListAdapter arr = l.getAdapter();
        Cursor thingy = (Cursor) arr.getItem(position);
        String n = thingy.getString(thingy.getColumnIndex(DbAdapter.KEY_SORTBY));

        Intent i = new Intent(this, DisplayItems.class);
        i.putExtra(EXTRA_NAME, n);
        i.putExtra(EXTRA_TYPE, authorType);
        startActivityForResult(i, REQUEST_ITEMS);
    }
}
