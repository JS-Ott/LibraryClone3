package jso.libcat3.display;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import jso.libcat3.R;
import jso.libcat3.db.DbAdapter;
import jso.libcat3.db.DbSaveManager;
import jso.libcat3.form.EntryForm;

public abstract class DisplaySuperclass extends ListActivity {
    // intent codes
    public static final int REQUEST_FORM = 0;       // EntryForm
    public static final int REQUEST_SEARCH = 1;     // SearchForm
    public static final int REQUEST_AUTHORS = 2;    // DisplayAuthors
    public static final int REQUEST_ITEMS = 3;      // DisplayItems
    public static final int REQUEST_FILE = 4;       // get a file
    public static final int REQUEST_SETTINGS = 5;   // settings

    // menu IDs
    public static final int MENU_DELETE = Menu.FIRST;

    // extra keys for intents
    public static final String EXTRA_NAME = "AUTHOR_NAME";
    public static final String EXTRA_ID = "_ID";
    public static final String EXTRA_DRILLDOWN = "DRILLDOWN";
    public static final String EXTRA_TYPE = "TYPE";

    protected DbAdapter dbAdapter;
    protected DbSaveManager dbSaveMgr;

    protected String authorName = null; // for activities
    protected int authorType;

    protected Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.display_listview);

        addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(DisplaySuperclass.this, EntryForm.class);
                i.putExtra(EXTRA_NAME, authorName);
                startActivityForResult(i, REQUEST_FORM);
            }
        });

        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        dbSaveMgr = new DbSaveManager(dbAdapter);

        int numBooks = dbAdapter.countBooks();
        setTitle(getResources().getText(R.string.app_name) + " (" + numBooks + " books)");

        auxCreate();

        fillData();
        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();

        fillData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dbAdapter.close();
    }

    protected abstract void auxCreate();

    protected abstract void fillData();

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 42:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    confirmImport();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode) {
            case (REQUEST_FILE):
                if (resultCode == RESULT_OK) {
                    List<String> pathSegments = intent.getData().getPathSegments();
                    String path = "";
                    for (int idx = 1; idx < pathSegments.size(); idx++) {
                        path = path + "/" + pathSegments.get(idx);
                    }

                    if (path.endsWith(".db")) {
                        try {
                            dbSaveMgr.importDb(path);
                            this.fillData();
                        } catch (Exception e) {
                            messagePopup(this, "There was a problem importing " + path);
                        }

                    }
                }

                fillData();
                break;
            case (REQUEST_SETTINGS):
            case (REQUEST_FORM):
                fillData();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_export):
                dbSaveMgr.exportDb(getDatabasePath(DbAdapter.DB_NAME));
                return true;
            case (R.id.action_import):
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 42);
                    }
                } else {
                    confirmImport();
                }

                //confirmImport();
                return true;
            case (R.id.action_stats):
                showStatistics();
                return true;
            case (R.id.action_reload): // TODO move into settings (with confirmation dialog)
                reloadVirtualTables();
                return true;
            case (R.id.action_clear): // TODO move into settings (with confirmation dialog)
                confirmClearCatalog();
                return true;
            case (R.id.action_settings):
                showSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void confirmImport() {
        AlertDialog.Builder imp = new AlertDialog.Builder(this);
        imp.setTitle("Confirm import");
        imp.setMessage("Importing from *.db will erase the current database!");

        imp.setPositiveButton("Continue",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.setType("file/*");
                        startActivityForResult(i, REQUEST_FILE);
                    }
                });

        imp.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // do nothing;
            }
        });

        imp.show();
    }

    protected void showStatistics() {
        Toast.makeText(this, "Sorry, still working on this", Toast.LENGTH_SHORT).show();
    }

    protected void showSettings() {
        startActivityForResult(new Intent(DisplaySuperclass.this, SettingsAct.class), REQUEST_SETTINGS);
    }

    protected void reloadVirtualTables() {
        dbAdapter.reloadVirtualTables();
    }

    protected void confirmClearCatalog() {
        AlertDialog.Builder clear = new AlertDialog.Builder(this);
        clear.setTitle("Confirm clear catalog");
        clear.setMessage("Are you absolutely for-real 100% sure you want to delete everything?");

        clear.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbAdapter.clearCatalog();
                fillData();
            }
        });

        clear.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        clear.show();
    }

    public static void messagePopup(Context context, String message) {
        AlertDialog.Builder foo = new AlertDialog.Builder(context);
        foo.setMessage(message);
        foo.setCancelable(false);
        foo.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        (foo.create()).show();
    }

    /**
     * Settings fragment helper class
     */
    public static class SettingsFrag extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    public static class SettingsAct extends Activity {
        public static String PREF_KEY_VIEWMODE = "pref_viewmode";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFrag())
                    .commit();
        }
    }
}
