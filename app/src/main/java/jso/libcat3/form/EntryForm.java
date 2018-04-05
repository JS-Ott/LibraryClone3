package jso.libcat3.form;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

import jso.libcat3.R;
import jso.libcat3.db.DbAdapter;
import jso.libcat3.db.ThreeRingBinder;
import jso.libcat3.display.DisplaySuperclass;

public class EntryForm extends Activity {
    private DbAdapter dbAdapter;

    //private TextView headerView;

    private RadioGroup typeView;

    private EditText titleView;
    private EditText authorView;
    private EditText sortByView;
    private EditText seriesView;
    private EditText numberView;

    private CheckBox ebookView;
    private CheckBox hardcopyView;

    Button showRelativesButton;

    private Long rowId = null;
    private String authorName;

    private ArrayList<Long> displayRelatives = new ArrayList<Long>();
    private ArrayList<Long> addedRelatives = new ArrayList<Long>();
    private ArrayList<Long> deletedRelatives = new ArrayList<Long>();

    private boolean drillDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        if (i.hasExtra(DisplaySuperclass.EXTRA_NAME)) {
            authorName = i.getStringExtra(DisplaySuperclass.EXTRA_NAME);
        } else {
            authorName = null;
        }

        if (i.hasExtra(DisplaySuperclass.EXTRA_DRILLDOWN)) {
            drillDown = i.getBooleanExtra(DisplaySuperclass.EXTRA_DRILLDOWN,
                    false);
        }

        dbAdapter = new DbAdapter(this);
        dbAdapter.open();

        setContentView(R.layout.layout_form);

        // initialize layout elements
        typeView = (RadioGroup) findViewById(R.id.type_group);

        titleView = (EditText) findViewById(R.id.title_text);
        authorView = (EditText) findViewById(R.id.author_text);

        sortByView = (EditText) findViewById(R.id.sortby_text);
        seriesView = (EditText) findViewById(R.id.series_text);
        numberView = (EditText) findViewById(R.id.number_text);

        ebookView = (CheckBox) findViewById(R.id.ebook_check);
        hardcopyView = (CheckBox) findViewById(R.id.hardcopy_check);

        showRelativesButton = (Button) findViewById(R.id.relatives_btn);
        showRelativesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final String[] from = new String[] { DbAdapter.KEY_TITLE,
                        DbAdapter.KEY_AUTHOR, DbAdapter.KEY_FORMATEBOOK,
                        DbAdapter.KEY_FORMATHARDCOPY };
                final int[] to = new int[] { R.id.tv_title, R.id.tv_author,
                        R.id.img_left, R.id.img_right };

                final ListView listview = new ListView(EntryForm.this);
                listview.setPadding(4, 4, 4, 4);

                Dialog dialog = new Dialog(EntryForm.this);
                dialog.setTitle("Relatives");

                if (displayRelatives.size() != 0) {

                    Cursor relatives = dbAdapter.getSetMinimal(displayRelatives);

                    if (relatives.getCount() != 0) {
                        SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(EntryForm.this,
                                R.layout.row_item, relatives, from, to);
                        cAdapter.setViewBinder(new ThreeRingBinder());

                        listview.setAdapter(cAdapter);

                        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                                Log.d("LONG TAP", String.valueOf(l));

                                final Long rId = l;

                                AlertDialog.Builder delete = new AlertDialog.Builder(EntryForm.this);
                                delete.setTitle("Confirm deletion");
                                delete.setMessage("Delete this item?");

                                delete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // delete the item
                                        deletedRelatives.add(rId);

                                        displayRelatives.remove(rId);
                                        addedRelatives.remove(rId);

                                        Cursor relatives = dbAdapter.getSetMinimal(displayRelatives);
                                        SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(EntryForm.this,
                                                R.layout.row_item, relatives, from, to);
                                        cAdapter.setViewBinder(new ThreeRingBinder());
                                        listview.setAdapter(cAdapter);

                                        fillRelatives();
                                    }
                                });

                                delete.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // do nothing
                                    }
                                });

                                delete.show();

                                return true;
                            }
                        });

                        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int id, long l) {
                                if (!drillDown) {
                                    Log.d("SINGLE TAP", String.valueOf(l));

                                    Intent i = new Intent(EntryForm.this, EntryForm.class);
                                    i.putExtra(DbAdapter.KEY_ID, l);
                                    i.putExtra(DisplaySuperclass.EXTRA_DRILLDOWN, true);
                                    startActivityForResult(i, DisplaySuperclass.REQUEST_FORM);
                                }
                            }
                        });

                        dialog.setContentView(listview);
                    }
                } else {
                    // show a "No relatives" message
                }

                dialog.show();
            }
        });

        Button addRelativeButton = (Button) findViewById(R.id.add_relative_btn);
        addRelativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int viewType = typeView.getCheckedRadioButtonId();
                int type;
                if (viewType == R.id.type_anthology) {
                    type = DbAdapter.TypeAnthology;
                } else if (viewType == R.id.type_book) {
                    type = DbAdapter.TypeBook;
                } else if (viewType == R.id.type_story) {
                    type = DbAdapter.TypeStory;
                } else {
                    DisplaySuperclass.messagePopup(EntryForm.this, "Pick a radiobutton");
                    return;
                }

                Intent i = new Intent(EntryForm.this, SearchForm.class);
                i.putExtra(DisplaySuperclass.EXTRA_TYPE, type);
                EntryForm.this.startActivityForResult(i, DisplaySuperclass.REQUEST_SEARCH);
            }
        });

        Button saveButton = (Button) findViewById(R.id.save_btn);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean succeeded = saveItem();
                if (succeeded) {
                    Intent i = new Intent();
                    setResult(RESULT_OK, i);
                    finish();
                }
            }
        });

        typeView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.type_story) {
                    // hide series, uncheck and hide slushpile (will that force hiding?)
                    seriesView.setVisibility(View.GONE);
                    numberView.setVisibility(View.GONE);
                } else {
                    // show series, show slushpile
                    seriesView.setVisibility(View.VISIBLE);
                    numberView.setVisibility(View.VISIBLE);
                }
            }
        });

        if (authorName != null) {
            sortByView.setText(authorName);
            if (authorName.contains(", ")) {
                String[] split = authorName.split(", ");
                authorView.setText(split[1] + " " + split[0]);
            } else {
                authorView.setText(authorName);
            }
        }

        rowId = (savedInstanceState == null) ? null : (Long) savedInstanceState
                .getSerializable(DbAdapter.KEY_ID);
        if (rowId == null) {
            Bundle extras = getIntent().getExtras();
            rowId = extras != null ? extras.getLong(DbAdapter.KEY_ID) : null;
        }

        populateFields();
        fillRelatives();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(DbAdapter.KEY_ID, rowId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbAdapter.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == DisplaySuperclass.REQUEST_SEARCH) {
            if (resultCode == RESULT_OK) {
                long relId = intent.getLongExtra(DisplaySuperclass.EXTRA_ID, -1);

                if (!addedRelatives.contains(relId) && !displayRelatives.contains(relId)) {
                    addedRelatives.add(relId);
                    displayRelatives.add(relId);
                }

                fillRelatives();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void populateFields() {
        if (rowId != null) {
            Cursor item = dbAdapter.getItem(rowId);
            startManagingCursor(item);

            if (item.getCount() != 0) {
                int type = item.getInt(item.getColumnIndex(DbAdapter.KEY_TYPE));
                if (type == DbAdapter.TypeBook) {
                    typeView.check(R.id.type_book);
                } else if (type == DbAdapter.TypeAnthology) {
                    typeView.check(R.id.type_anthology);
                } else if (type == DbAdapter.TypeStory) {
                    typeView.check(R.id.type_story);
                }

                if (type != DbAdapter.TypeStory) {
                    seriesView.setVisibility(View.VISIBLE);
                    String series = item.getString(item.getColumnIndex(DbAdapter.KEY_SERIES));
                    seriesView.setText(series);

                    numberView.setVisibility(View.VISIBLE);
                    String seriesNumber = item.getString(item.getColumnIndex(DbAdapter.KEY_SERIESNUMBER));
                    numberView.setText(seriesNumber);
                } else {
                    seriesView.setVisibility(View.GONE);
                    numberView.setVisibility(View.GONE);
                }

                String title = item.getString(item.getColumnIndex(DbAdapter.KEY_TITLE));
                titleView.setText(title);
                setTitle("Editing " + titleView.getText().toString());

                String author = item.getString(item.getColumnIndex(DbAdapter.KEY_AUTHOR));
                authorView.setText(author);

                String sortBy = item.getString(item.getColumnIndex(DbAdapter.KEY_SORTBY));
                sortByView.setText(sortBy);

                ebookView.setChecked(item.getInt(item.getColumnIndex(DbAdapter.KEY_FORMATEBOOK)) == 1);
                hardcopyView.setChecked(item.getInt(item.getColumnIndex(DbAdapter.KEY_FORMATHARDCOPY)) == 1);

                // populate relatives from database
                Cursor relatives;
                long rId;
                if (type < DbAdapter.TypeStory) {
                    relatives = dbAdapter.getChildren(rowId);

                    while (!relatives.isAfterLast()) {
                        rId = relatives.getLong(relatives.getColumnIndex(DbAdapter.KEY_CHILD));
                        /* I don't know why, but populateFields gets called twice,
                           so it's necessary to make sure that the relative isn't
                           already present */
                        if (!displayRelatives.contains(rId)) {
                            displayRelatives.add(rId);
                        }
                        relatives.moveToNext();
                    }
                } else {
                    relatives = dbAdapter.getParents(rowId);

                    while (!relatives.isAfterLast()) {
                        rId = relatives.getLong(relatives.getColumnIndex(DbAdapter.KEY_PARENT));
                        if (!displayRelatives.contains(rId)) {
                            displayRelatives.add(rId);
                        }
                        relatives.moveToNext();
                    }
                }
            } else {
                // what happens if I remove this line?
                rowId = null;
                // DON'T. JUST DON'T. BECAUSE OF REASONS.
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void fillRelatives() {
        showRelativesButton.setText(displayRelatives.size() + " relatives");
    }

    private boolean saveItem() {
        int viewType = typeView.getCheckedRadioButtonId();
        int type;
        if (viewType == R.id.type_anthology) {
            type = DbAdapter.TypeAnthology;
        } else if (viewType == R.id.type_book) {
            type = DbAdapter.TypeBook;
        } else if (viewType == R.id.type_story) {
            type = DbAdapter.TypeStory;
        } else {
            DisplaySuperclass.messagePopup(this, "Pick a radiobutton");
            return false;
        }

        String title = titleView.getText().toString();
        if (isBlank(title)) {
            DisplaySuperclass.messagePopup(this, "Enter a title");
            return false;
        }

        String author = authorView.getText().toString();

        String sortBy = sortByView.getText().toString();
        if (isBlank(sortBy)) {
            DisplaySuperclass.messagePopup(this, "Enter a value to sort by");
            return false;
        }

        String series = seriesView.getText().toString();

        String sNum = numberView.getText().toString();
        sNum = "".equals(sNum) ? null : sNum;

        boolean ebook = ebookView.isChecked();
        boolean hardcopy = hardcopyView.isChecked();

        if (rowId == null) {
            long id = -1;

            // create
            id = dbAdapter.createItem(title, author, sortBy, series, sNum, ebook, hardcopy, type);

            // set rowId
            if (id > 0) {
                rowId = id;
            }

            for (Long relId : displayRelatives) {
                if (type < DbAdapter.TypeStory) {
                    dbAdapter.createRelationship(rowId, relId);
                } else {
                    dbAdapter.createRelationship(relId, rowId);
                }
            }

            return rowId > 0;
        } else {
            // update
            boolean success = dbAdapter.updateItem(rowId, title, author, sortBy, series, sNum,
                    ebook, hardcopy, type);

            for (Long aId : addedRelatives) {
                if (type < DbAdapter.TypeStory) {
                    dbAdapter.createRelationship(rowId, aId);
                } else {
                    dbAdapter.createRelationship(aId, rowId);
                }
            }
            for (Long dId : deletedRelatives) {
                if (type < DbAdapter.TypeStory) {
                    dbAdapter.deleteRelationship(rowId, dId);
                } else {
                    dbAdapter.deleteRelationship(dId, rowId);
                }
            }

            return success;
        }
    }

    public static boolean isBlank(String check) {
        // String.isEmpty() requires API min 9; B&N Nook runs on 7
        return check == null || check.trim().length() == 0;
    }
}
