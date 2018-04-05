package jso.libcat3.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import jso.libcat3.form.EntryForm;

public class DbAdapter {
    // column names
    public static final String KEY_ID = "_id";
    public static final String KEY_TITLE = "Title";
    public static final String KEY_SERIES = "Series";
    public static final String KEY_COUNT = "Count";
    // Library
    public static final String KEY_TITLEARTICLE = "TitleArticle";
    public static final String KEY_TITLEREST = "TitleRest";
    public static final String KEY_AUTHOR = "Author";
    public static final String KEY_SORTBY = "SortBy";
    public static final String KEY_SERIESARTICLE = "SeriesArticle";
    public static final String KEY_SERIESREST = "SeriesRest";
    public static final String KEY_SERIESNUMBER = "SeriesNumber";
    public static final String KEY_FORMATEBOOK = "FormatEbook";
    public static final String KEY_FORMATHARDCOPY = "FormatHardcopy";
    public static final String KEY_TYPE = "Type";
    // Relationships
    public static final String KEY_PARENT = "Parent";
    public static final String KEY_CHILD = "Child";
    // VT
    public static final String KEY_LIBRARYID = "LibraryId";

    // threshold for sorting
    public static final int ThresholdBook = 5;
    public static final int ThresholdStory = 10;
    // type of volume
    public static final int TypeAnthology = 1;
    public static final int TypeBook = 2;
    public static final int TypeStory = 3;

    private DbHelper dbHelper;
    private SQLiteDatabase db;
    private Context ctx;

    private static final int DB_VERSION = 1;

    // database
    public static final String DB_NAME = "libcat3_db";
    // tables
    public static final String TABLE_LIBRARY = "Library";
    public static final String TABLE_RELATIONSHIPS = "Relationships";
    // views
    private static final String VT_LIBRARY = "virtual_library";

    // table creation statements
    /**
    CREATE TABLE Library (
      _id integer primary key autoincrement,
      TitleArticle text,
      TitleRest text not null,
      Author text,
      SortBy text not null,
      SeriesArticle text,
      SeriesRest text,
      SeriesNumber integer,
      FormatEbook integer,
      FormatHardcopy integer,
      Type integer
    );
     */
     private static final String CREATE_TABLE_LIBRARY = "CREATE TABLE " + TABLE_LIBRARY + " ( "
            + KEY_ID + " integer primary key autoincrement, " + KEY_TITLEARTICLE + " text, "
            + KEY_TITLEREST + " text not null, " + KEY_AUTHOR + " text, " + KEY_SORTBY
            + " text not null, " + KEY_SERIESARTICLE + " text, " + KEY_SERIESREST + " text, "
            + KEY_SERIESNUMBER + " integer, " + KEY_FORMATEBOOK + " integer, " + KEY_FORMATHARDCOPY
            + " integer, " + KEY_TYPE + " integer );";
    /**
    CREATE TABLE Relationships (
      _id integer primary key autoincrement,
      Parent integer not null,
      Child integer not null
    );
     */
    private static final String CREATE_TABLE_RELATIONSHIPS = "CREATE TABLE " + TABLE_RELATIONSHIPS
            + " ( " + KEY_ID + " integer primary key autoincrement, " + KEY_PARENT
            + " integer not null, " + KEY_CHILD + " integer not null );";
    /**
    CREATE VIRTUAL TABLE virtual_library using fts3 (
      _id integer primary key autoincrement,
      LibraryId integer not null,
      Title text,
      Author text,
      Series text
    );
     */
    private static final String CREATE_VT_LIBRARY = "CREATE VIRTUAL TABLE " + VT_LIBRARY
            + " using fts3 ( " + KEY_ID + " integer primary key autoincrement, "
            + KEY_LIBRARYID + " integer not null, " + KEY_TITLEARTICLE + " text, " + KEY_TITLEREST
            + " text, " + KEY_AUTHOR + " text );";

    public DbAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public DbAdapter open() throws SQLException {
        dbHelper = new DbHelper(ctx);
        db = dbHelper.getWritableDatabase();

        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void clearCatalog() {
        db.delete(TABLE_LIBRARY, null, null);
        db.delete(TABLE_RELATIONSHIPS, null, null);
        db.delete(VT_LIBRARY, null, null);
    }

    // Library + Relationships CRUD

    public long createItem(String title, String author, String sortBy, String series,
                           String seriesNumber, boolean formatEbook, boolean formatHardcopy, int type) {
        ContentValues args = new ContentValues();

        String[] t = article(title);
        String[] s = article(series);

        args.put(KEY_TITLEARTICLE, t[0]);
        args.put(KEY_TITLEREST, t[1]);
        args.put(KEY_AUTHOR, author);
        args.put(KEY_SORTBY, sortBy);

        if (type != TypeStory) {
            args.put(KEY_SERIESARTICLE, s[0]);
            args.put(KEY_SERIESREST, s[1]);
            if (seriesNumber == null) args.putNull(KEY_SERIESNUMBER);
            else args.put(KEY_SERIESNUMBER, Integer.parseInt(seriesNumber));
        } else {
            args.putNull(KEY_SERIESARTICLE);
            args.putNull(KEY_SERIESREST);
            args.putNull(KEY_SERIESNUMBER);
        }

        args.put(KEY_FORMATEBOOK, formatEbook ? 1 : 0);
        args.put(KEY_FORMATHARDCOPY, formatHardcopy ? 1 : 0);
        args.put(KEY_TYPE, type);

        long id = db.insert(TABLE_LIBRARY, null, args);
        if (id > 0) createVirtualItem(id, t[0], t[1], author);

        return id;
    }

    public long createRelationship(long parentId, long childId) {
        ContentValues args = new ContentValues();

        args.put(KEY_PARENT, parentId);
        args.put(KEY_CHILD, childId);

        return db.insert(TABLE_RELATIONSHIPS, null, args);
    }

    public boolean updateItem(long id, String title, String author, String sortBy, String series,
                           String seriesNumber, boolean formatEbook, boolean formatHardcopy, int type) {
        ContentValues args = new ContentValues();

        String[] t = article(title);
        String[] s = article(series);

        args.put(KEY_TITLEARTICLE, t[0]);
        args.put(KEY_TITLEREST, t[1]);
        args.put(KEY_AUTHOR, author);
        args.put(KEY_SORTBY, sortBy);

        if (type != TypeStory) {
            args.put(KEY_SERIESARTICLE, s[0]);
            args.put(KEY_SERIESREST, s[1]);
            if (seriesNumber == null) args.putNull(KEY_SERIESNUMBER);
            else args.put(KEY_SERIESNUMBER, Integer.parseInt(seriesNumber));
        } else {
            args.putNull(KEY_SERIESARTICLE);
            args.putNull(KEY_SERIESREST);
            args.putNull(KEY_SERIESNUMBER);
        }

        args.put(KEY_FORMATEBOOK, formatEbook ? 1 : 0);
        args.put(KEY_FORMATHARDCOPY, formatHardcopy ? 1 : 0);
        args.put(KEY_TYPE, type);

        int result = db.update(TABLE_LIBRARY, args, KEY_ID + "=" + id, null);
        if (result > 0) updateVirtualItem(id, t[0], t[1], author);

        return result > 0;
    }

    public boolean deleteItem(long id) {
        long result = db.delete(TABLE_LIBRARY, KEY_ID + "=?", new String[] {String.valueOf(id)});

        if (result > 0) {
            deleteVirtualItem(id);
            deleteChildRelationships(id);
            deleteParentRelationships(id);
        }

        return result > 0;
    }

    public boolean deleteRelationship(long parentId, long childId) {
        return db.delete(TABLE_RELATIONSHIPS, KEY_PARENT + "=" + parentId
                + " and " + KEY_CHILD + "=" + childId, null) > 0;
    }

    private boolean deleteChildRelationships(long parentId) {
        return db
                .delete(TABLE_RELATIONSHIPS, KEY_PARENT + "=" + parentId, null) > 0;
    }

    private boolean deleteParentRelationships(long childId) {
        return db.delete(TABLE_RELATIONSHIPS, KEY_CHILD + "=" + childId, null) > 0;
    }

    /**
     * select
     *   Library._id as _id,
     *   TitleArticle||TitleRest as Title,
     *   Author,
     *   SortBy,
     *   SeriesArticle||SeriesRest as Series,
     *   SeriesNumber,
     *   FormatEbook,
     *   FormatHardcopy,
     *   Type
     * from Library
     * where Library._id=?
     * limit 1
     */
    public Cursor getItem(long id) {
        String sql;
        sql = "select " + TABLE_LIBRARY + "." + KEY_ID + " as " + KEY_ID + ", " + KEY_TITLEARTICLE
                + "||" + KEY_TITLEREST + " as " + KEY_TITLE + ", " + KEY_AUTHOR + ", " + KEY_SORTBY
                + ", " + KEY_SERIESARTICLE + "||" + KEY_SERIESREST + " as " + KEY_SERIES + ", "
                + KEY_SERIESNUMBER + ", " + KEY_FORMATEBOOK + ", " + KEY_FORMATHARDCOPY + ", "
                + KEY_TYPE + " from " + TABLE_LIBRARY + " where " + TABLE_LIBRARY + "."
                + KEY_ID + "=? limit 1";
        Cursor c = db.rawQuery(sql, new String[] {String.valueOf(id)});
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   TitleArticle||TitleRest as Title,
     *   Author,
     *   FormatEbook,
     *   FormatHardcopy
     * from Library
     * where _id in (ids)
     * order by
     *   SortBy,
     *   TitleRest
     */
    public Cursor getSetMinimal(ArrayList<Long> ids) {
        String foo = ids.toString();
        String idString = foo.substring(1, foo.length()-1);
        String sql = "select " + KEY_ID + ", " + KEY_TITLEARTICLE + "||" + KEY_TITLEREST + " as "
                + KEY_TITLE + ", " + KEY_AUTHOR + ", " + KEY_FORMATEBOOK + ", " + KEY_FORMATHARDCOPY
                + " from " + TABLE_LIBRARY + " where " + KEY_ID + " in (" + idString + ")"
                + " order by " + KEY_SORTBY + ", " + KEY_TITLEREST;
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   Library._id as _id,
     *   TitleArticle||TitleRest as Title,
     *   Author,
     *   SortBy,
     *   SeriesArticle||SeriesRest as Series,
     *   SeriesNumber,
     *   FormatEbook,
     *   FormatHardcopy,
     *   Type
     * from Library
     * where Type=1
     * order by
     *   (case when SeriesNumber is null then 1 else 0 end),
     *   seriesRest,
     *   SeriesNumber,
     *   SortBy,
     *   TitleRest
     */
    public Cursor getAllAnthologies() {
        String sql = "select " + TABLE_LIBRARY + "." + KEY_ID + " as " + KEY_ID + ", "
                + KEY_TITLEARTICLE + "||" + KEY_TITLEREST + " as " + KEY_TITLE + ", " + KEY_AUTHOR
                + ", " + KEY_SORTBY + ", " + KEY_SERIESARTICLE + "||" + KEY_SERIESREST + " as "
                + KEY_SERIES + ", " + KEY_SERIESNUMBER + ", " + KEY_FORMATEBOOK + ", "
                + KEY_FORMATHARDCOPY + ", " + KEY_TYPE + " from " + TABLE_LIBRARY + " where " + KEY_TYPE
                + "=" + TypeAnthology + " order by " + " ( case when " + KEY_SERIESNUMBER
                + " is null then 1 else 0 end ), " + KEY_SERIESREST + ", " + KEY_SERIESNUMBER + ", "
                + KEY_SORTBY + ", " + KEY_TITLEREST;
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   Library._id as _id,
     *   TitleArticle||TitleRest as Title,
     *   Author,
     *   SortBy,
     *   SeriesArticle||SeriesRest as Series,
     *   SeriesNumber,
     *   FormatEbook,
     *   FormatHardcopy,
     *   Type
     * from Library
     * where
     *   Type=2
     *   and
     *   SortBy=?
     * order by
     *   (case when SeriesNumber is null then 1 else 0 end),
     *   seriesRest,
     *   SeriesNumber,
     *   TitleRest
     */
    public Cursor getBooksBy(String sortBy) {
        String sql = "select " + TABLE_LIBRARY + "." + KEY_ID + " as " + KEY_ID + ", "
                + KEY_TITLEARTICLE + "||" + KEY_TITLEREST + " as " + KEY_TITLE + ", " + KEY_AUTHOR
                + ", " + KEY_SORTBY + ", " + KEY_SERIESARTICLE + "||" + KEY_SERIESREST + " as "
                + KEY_SERIES + ", " + KEY_SERIESNUMBER + ", " + KEY_FORMATEBOOK + ", "
                + KEY_FORMATHARDCOPY + ", " + KEY_TYPE + " from " + TABLE_LIBRARY + " where " + KEY_TYPE
                + "=" + TypeBook + " and " + KEY_SORTBY + "=?" + " order by ( case when "
                + KEY_SERIESNUMBER + " is null then 1 else 0 end ), " + KEY_SERIESREST + ", "
                + KEY_SERIESNUMBER + ", " + KEY_TITLEREST;
        Cursor c = db.rawQuery(sql, new String[] {sortBy});
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   TitleArticle||TitleRest as Title,
     *   Author,
     *   SortBy,
     *   FormatEbook,
     *   FormatHardcopy,
     *   Type
     * from Library
     * where
     *   Type=3
     *   and
     *   SortBy=?
     * order by
     *   TitleRest
     */
    public Cursor getStoriesBy(String sortBy) {
        String sql = "select " + KEY_ID + ", " + KEY_TITLEARTICLE + "||" + KEY_TITLEREST + " as "
                + KEY_TITLE + ", " + KEY_AUTHOR + ", " + KEY_SORTBY + ", " + KEY_FORMATEBOOK
                + ", " + KEY_FORMATHARDCOPY + ", " + KEY_TYPE + " from " + TABLE_LIBRARY
                + " where " + KEY_TYPE + "=" + TypeStory + " and " + KEY_SORTBY + "=?"
                + " order by " + KEY_TITLEREST;
        Cursor c = db.rawQuery(sql, new String[] {sortBy});
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   SortBy,
     *   count(SortBy) as Count
     * from Library
     * where type=2
     * group by SortBy
     * order by
     *   (case when Count>=5 then 0 else 1 end),
     *   SortBy,
     *   Count desc
     */
    public Cursor getBookAuthorsWithCount() {
        String sql = "select " + KEY_ID + ", " + KEY_SORTBY + ", count(" + KEY_SORTBY + ") as "
                + KEY_COUNT + " from " + TABLE_LIBRARY + " where " + KEY_TYPE + "=" + TypeBook
                + " group by " + KEY_SORTBY + " order by (case when " + KEY_COUNT + ">="
                + ThresholdBook + " then 0 else 1 end), " + KEY_SORTBY + ", " + KEY_COUNT + " desc";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   SortBy,
     *   count(SortBy) as Count
     * from Library
     * where type=3
     * group by SortBy
     * order by
     *   (case when Count>=10 then 0 else 1 end),
     *   SortBy,
     *   Count desc
     */
    public Cursor getStoryAuthorsWithCount() {
        String sql = "select " + KEY_ID + ", " + KEY_SORTBY + ", count(" + KEY_SORTBY + ") as "
                + KEY_COUNT + " from " + TABLE_LIBRARY + " where " + KEY_TYPE + "=" + TypeStory
                + " group by " + KEY_SORTBY + " order by (case when " + KEY_COUNT + ">="
                + ThresholdStory + " then 0 else 1 end), " + KEY_SORTBY + ", " + KEY_COUNT
                + " desc";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   Parent,
     *   Child
     *   from Relationships
     * where Parent=?
     */
    public Cursor getChildren(long parentId) {
        String sql = "select " + KEY_ID + ", " + KEY_PARENT + ", " + KEY_CHILD + " from "
                + TABLE_RELATIONSHIPS + " where " + KEY_PARENT + "=?";
        Cursor c = db.rawQuery(sql, new String[] {String.valueOf(parentId)});
        c.moveToFirst();
        return c;
    }

    /**
     * select
     *   _id,
     *   Parent,
     *   Child
     *   from Relationships
     * where Child=?
     */
    public Cursor getParents(long childId) {
        String sql = "select " + KEY_ID + ", " + KEY_PARENT + ", " + KEY_CHILD + " from "
                + TABLE_RELATIONSHIPS + " where " + KEY_CHILD + "=?";
        Cursor c = db.rawQuery(sql, new String[] {String.valueOf(childId)});
        c.moveToFirst();
        return c;
    }

    // virtual library CRUD

    private long createVirtualItem(long libId, String titleArticle, String titleRest, String author) {
        ContentValues args = new ContentValues();


        args.put(KEY_LIBRARYID, libId);
        args.put(KEY_TITLEARTICLE, titleArticle);
        args.put(KEY_TITLEREST, titleRest);
        args.put(KEY_AUTHOR, author);

        return db.insert(VT_LIBRARY, null, args);
    }

    private boolean updateVirtualItem(long libId, String titleArticle, String titleRest, String author) {
        ContentValues args = new ContentValues();

        args.put(KEY_LIBRARYID, libId);
        args.put(KEY_TITLEARTICLE, titleArticle);
        args.put(KEY_TITLEREST, titleRest);
        args.put(KEY_AUTHOR, author);

        return db.update(VT_LIBRARY, args, KEY_LIBRARYID + "=" + libId, null) > 0;
    }

    private boolean deleteVirtualItem(long libId) {
        return db.delete(VT_LIBRARY, KEY_LIBRARYID + "=" + libId, null) > 0;
    }

    /**
     * select
     *   _id,
     *   TitleArticle||TitleRest as Title,
     *   SortBy,
     *   Author,
     *   SeriesArticle||SeriesRest as Series
     *   from Library
     *   order by SortBy, TitleRest
     */
    public void reloadVirtualTables() {
        String sql = "select " + KEY_ID + ", " + KEY_TITLEARTICLE + ", " + KEY_TITLEREST + ", "
                + KEY_SORTBY + ", " + KEY_AUTHOR + " from " + TABLE_LIBRARY + " order by "
                + KEY_SORTBY + ", " + KEY_TITLEREST;
        Cursor all = db.rawQuery(sql, null);
        all.moveToFirst();

        db.delete(VT_LIBRARY, null, null);

        while (!all.isAfterLast()) {
            this.createVirtualItem(
                    all.getInt(all.getColumnIndex(KEY_ID)),
                    all.getString(all.getColumnIndex(KEY_TITLEARTICLE)),
                    all.getString(all.getColumnIndex(KEY_TITLEREST)),
                    all.getString(all.getColumnIndex(KEY_AUTHOR))
            );
            all.moveToNext();
        }
    }

    // searching virtual tables
    // select * from vt where vt match string
    // order by sortBy, titlePart

    /**
     * select
     *   virtual_library._id,
     *   LibraryId,
     *   Library.TitleArticle||Library.TitleRest as Title,
     *   Library.Author,
     *   SortBy,
     *   FormatEbook,
     *   FormatHardcopy,
     *   Type
     * from virtual library
     * inner join Library on LibraryId=Library._id
     * where
     *   virtual_library match ?
     *   and Type<3
     * order by
     *   Library.SortBy,
     *   Library.TitleRest
     */
    public Cursor searchCatalog(String searchString, boolean volume, boolean story) {
        if (EntryForm.isBlank(searchString)) return null;

        String sql = "select " + VT_LIBRARY + "." + KEY_ID + ", " + KEY_LIBRARYID + ", "
                + TABLE_LIBRARY + "." + KEY_TITLEARTICLE + "||" + TABLE_LIBRARY + "."
                + KEY_TITLEREST + " as " + KEY_TITLE + ", " + TABLE_LIBRARY + "." + KEY_AUTHOR
                + ", " + KEY_SORTBY + ", " + KEY_FORMATEBOOK + ", " + KEY_FORMATHARDCOPY + ", " + KEY_TYPE + " from "
                + VT_LIBRARY + " inner join " + TABLE_LIBRARY + " on " + KEY_LIBRARYID + "=" + TABLE_LIBRARY + "." + KEY_ID
                + " where " + VT_LIBRARY + " match ?";

        if (volume && !story) {
            sql += " and " + KEY_TYPE + "<" + TypeStory;
        } else if (story && !volume) {
            sql += " and " + KEY_TYPE + "=" + TypeStory;
        }

        sql += " order by " + KEY_SORTBY + ", " + TABLE_LIBRARY
                + "." + KEY_TITLEREST;
        return db.rawQuery(sql, new String[]{wildcard(searchString)});
    }

    // Stats generators

    public int countAll() {
        String sql = "select count (*) from " + TABLE_LIBRARY;
        SQLiteStatement stmt = db.compileStatement(sql);
        return (int)stmt.simpleQueryForLong();
    }

    public int countAnthologies() {
        String sql = "select count (*) from " + TABLE_LIBRARY + " where " + KEY_TYPE + "="
                + TypeAnthology;
        SQLiteStatement stmt = db.compileStatement(sql);
        return (int)stmt.simpleQueryForLong();
    }

    public int countBooks() {
        String sql = "select count (*) from " + TABLE_LIBRARY + " where " + KEY_TYPE + "="
                + TypeBook;
        SQLiteStatement stmt = db.compileStatement(sql);
        return (int)stmt.simpleQueryForLong();
    }

    public int countBooksDigital() {
        String sql = "select count(*) from " + TABLE_LIBRARY + " where " + KEY_TYPE + "=" + TypeBook
                + " and " + KEY_FORMATEBOOK + "=1";
        SQLiteStatement stmt = db.compileStatement(sql);
        return (int)stmt.simpleQueryForLong();
    }

    public int countStories() {
        String sql = "select count (*) from " + TABLE_LIBRARY + " where " + KEY_TYPE + "="
                + TypeStory;
        SQLiteStatement stmt = db.compileStatement(sql);
        return (int)stmt.simpleQueryForLong();
    }

    public int countStoriesBy(String sortBy) {
        String sql = "select count (*) from " + TABLE_LIBRARY + " where " + KEY_TYPE + "="
                + TypeStory + " and " + KEY_SORTBY + "=?";
        SQLiteStatement stmt = db.compileStatement(sql);
        stmt.bindString(1, sortBy);
        return (int)stmt.simpleQueryForLong();
    }

    /**
     * select distinct SortBy from Library where type=2
     */
    public int countBookAuthors() {
        String sql = "select distinct " + KEY_SORTBY + " from " + TABLE_LIBRARY + " where "
                + KEY_TYPE + "=?";
        String[] args = new String[] {String.valueOf(TypeBook)};
        Cursor c = db.rawQuery(sql, args);
        return c.getCount();
    }

    /**
     * select distinct SortBy from Library where type=3
     */
    public int countStoryAuthors() {
        String sql = "select distinct " + KEY_SORTBY + " from " + TABLE_LIBRARY + " where "
                + KEY_TYPE + "=?";
        String[] args = new String[] {String.valueOf(TypeStory)};
        Cursor c = db.rawQuery(sql, args);
        return c.getCount();
    }

    /**
     * select distinct SortBy from Library where type=1
     */
    public int countEditors() {
        String sql = "select distinct " + KEY_SORTBY + " from " + TABLE_LIBRARY + " where "
                + KEY_TYPE + "=?";
        String[] args = new String[] {String.valueOf(TypeAnthology)};
        Cursor c = db.rawQuery(sql, args);
        return c.getCount();
    }

    /**
     * takes in a complete title and checks to see if there's an article at the
     * beginning
     *
     * @param full
     *            the complete title
     * @return index 0 is article with whitespace, index 1 is the rest of the
     *         title
     */
    public static String[] article(String full) {
        if (full == null) return new String[] {"", ""};

        if (full.startsWith("The ")) {
            return new String[] { "The ", full.substring(4, full.length()) };
        } else if (full.startsWith("An ")) {
            return new String[] { "An ", full.substring(3, full.length()) };
        } else if (full.startsWith("A ")) {
            return new String[] { "A ", full.substring(2, full.length()) };
        } else {
            return new String[] { "", full };
        }
    }

    private static String wildcard(String input) {
        if (EntryForm.isBlank(input))
            return "";

        StringBuilder sb = new StringBuilder();
        String[] split = input.split(" ");

        for (String s : split)
            sb.append(s).append("*").append(" ");

        return sb.toString().trim();
    }

    public void importNewLibrary(String path) {
        this.clearCatalog();

        File importFile = new File(path);

        db.execSQL("attach ? as NewLibrary", new String[] { path });
        // libcat3_db
        db.execSQL("insert into Library select * from NewLibrary.Library");
        db.execSQL("insert into Relationships select * from NewLibrary.Relationships");

        this.reloadVirtualTables();
    }

    // DbHelper class
    private static class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_LIBRARY);
            db.execSQL(CREATE_TABLE_RELATIONSHIPS);
            db.execSQL(CREATE_VT_LIBRARY);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("DbAdapter", "Upgrading database from version " + oldVersion
                    + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIBRARY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RELATIONSHIPS);
            db.execSQL("DROP TABLE IF EXISTS " + VT_LIBRARY);
            onCreate(db);
        }
    }
}

