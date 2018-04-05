package jso.libcat3.db;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import jso.libcat3.R;

public class ThreeRingBinder implements SimpleCursorAdapter.ViewBinder {

    @Override
    public boolean setViewValue(View view, Cursor cursor, int colIdx) {
        if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_TITLE)) {
            String ttl = cursor.getString(colIdx);
            if (!"".equals(ttl) && ttl != null) {
                ((TextView) view).setText(ttl);
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
                return false;
            }
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_FORMATEBOOK)) {
            ((ImageView) view).setImageResource(R.drawable.floppy);
            if (cursor.getInt(colIdx) == 1) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.INVISIBLE);
            }
            return true;
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_FORMATHARDCOPY)) {
            ((ImageView) view).setImageResource(R.drawable.book);
            if (cursor.getInt(colIdx) == 1) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.INVISIBLE);
            }
            return true;
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_SERIES)) {
            String ser = cursor.getString(colIdx);
            if (!"".equals(ser) && ser != null) {
                ((TextView) view).setText(ser);
                view.setVisibility(View.VISIBLE);
                return true;
            } else {
                view.setVisibility(View.GONE);
                return false;
            }
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_SERIESNUMBER)) {
            String num = cursor.getString(colIdx);
            if (!("".equals(num)) && num != null) {
                ((TextView) view).setText("#" + num);
                view.setVisibility(View.VISIBLE);
                return true;
            } else {
                view.setVisibility(View.GONE);
                return false;
            }
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_AUTHOR)) {
            String aut = cursor.getString(colIdx);
            if (!("".equals(aut)) && aut != null) {
                ((TextView) view).setText(aut);
                view.setVisibility(View.VISIBLE);
                return true;
            } else {
                view.setVisibility(View.GONE);
                return false;
            }
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_SORTBY)) {
            String sby = cursor.getString(colIdx);
            if (!("".equals(sby)) && sby != null) {
                ((TextView) view).setText(sby);
                view.setVisibility(View.VISIBLE);
                return true;
            } else {
                view.setVisibility(View.GONE);
                return false;
            }
        } else if (colIdx == cursor.getColumnIndex(DbAdapter.KEY_COUNT)) {
            String count = cursor.getString(colIdx);
            TextView tv = (TextView) view;
            tv.setText(" (" + count + ")");
            view.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }
}
