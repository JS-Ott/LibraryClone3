package jso.libcat3.display;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import jso.libcat3.R;
import jso.libcat3.db.DbAdapter;
import jso.libcat3.db.ThreeRingBinder;
import jso.libcat3.form.EntryForm;

public class DisplayItems extends DisplaySuperclass {
    private static String[] from;
    private static int[] to;

    @Override
    protected void auxCreate() {
        Intent i = getIntent();

        if (i.hasExtra(EXTRA_NAME)) {
            authorName = i.getStringExtra(EXTRA_NAME);
        } else {
            authorName = null;
        }

        authorType = i.getIntExtra(EXTRA_TYPE, -1);

        setFromTo(); // needs to be factored out so it can be re-called in case of settings change
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void fillData() {
        setFromTo();

        Cursor itemCursor;

        if (authorType == DbAdapter.TypeAnthology) {
            itemCursor = dbAdapter.getAllAnthologies();
        } else if (authorType == DbAdapter.TypeBook) {
            itemCursor = dbAdapter.getBooksBy(authorName);

            if (getListView().getHeaderViewsCount() == 0 && dbAdapter.countStoriesBy(authorName) > 0) {
                // link to short stories
                ListView lv = getListView();
                LayoutInflater inflater = getLayoutInflater();

                LinearLayout headerS = (LinearLayout) inflater.inflate(
                        R.layout.auxiliary_header, lv, false);
                TextView textS = (TextView) headerS.findViewById(R.id.aux_header);
                textS.setText("Short stories (" + dbAdapter.countStoriesBy(authorName) + ")");
                textS.setTypeface(null, Typeface.ITALIC);
                textS.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(DisplayItems.this, DisplayItems.class);
                        i.putExtra(EXTRA_NAME, authorName);
                        i.putExtra(EXTRA_TYPE, DbAdapter.TypeStory);
                        startActivityForResult(i, REQUEST_AUTHORS);
                    }
                });
                lv.addHeaderView(headerS, null, false);
            }
        } else {
            itemCursor = dbAdapter.getStoriesBy(authorName);
        }

        SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(this, R.layout.row_item,
                itemCursor, from, to);
        cAdapter.setViewBinder(new ThreeRingBinder());

        if (getListAdapter() == null) {
            setListAdapter(cAdapter);
        } else {
            ((SimpleCursorAdapter) getListAdapter()).swapCursor(itemCursor);
            ((SimpleCursorAdapter) getListAdapter()).notifyDataSetChanged();

            if (itemCursor.getCount() == 0) {
                setListAdapter(null);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, MENU_DELETE, 0, R.string.action_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (MENU_DELETE):
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                dbAdapter.deleteItem(info.id);
                fillData();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent i = new Intent(this, EntryForm.class);
        i.putExtra(DbAdapter.KEY_ID, id);
        startActivityForResult(i, REQUEST_FORM);
    }

    protected void setFromTo() {
        if (authorType == DbAdapter.TypeAnthology) {
            from = new String[]{
                    DbAdapter.KEY_TITLE,
                    DbAdapter.KEY_FORMATEBOOK,
                    DbAdapter.KEY_FORMATHARDCOPY,
                    DbAdapter.KEY_AUTHOR,
                    DbAdapter.KEY_SERIES,
                    DbAdapter.KEY_SERIESNUMBER
            };
            to = new int[]{
                    R.id.tv_title,
                    R.id.img_left,
                    R.id.img_right,
                    R.id.tv_author,
                    R.id.tv_series,
                    R.id.tv_snum
            };
        } else if (authorType == DbAdapter.TypeBook) {
            from = new String[]{
                    DbAdapter.KEY_TITLE,
                    DbAdapter.KEY_FORMATEBOOK,
                    DbAdapter.KEY_FORMATHARDCOPY,
                    DbAdapter.KEY_SERIES,
                    DbAdapter.KEY_SERIESNUMBER
            };
            to = new int[]{
                    R.id.tv_title,
                    R.id.img_left,
                    R.id.img_right,
                    R.id.tv_series,
                    R.id.tv_snum
            };
        } else {
            from = new String[]{
                    DbAdapter.KEY_TITLE,
                    DbAdapter.KEY_FORMATEBOOK,
                    DbAdapter.KEY_FORMATHARDCOPY
            };
            to = new int[]{
                    R.id.tv_title,
                    R.id.img_left,
                    R.id.img_right
            };
        }
    }
}
