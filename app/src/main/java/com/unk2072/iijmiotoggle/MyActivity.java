package com.unk2072.iijmiotoggle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class MyActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MyActivity";
    private String[] mListText = new String[4];
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initListView();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String token = pref.getString(Const.ACCESS_TOKEN, "");

        if (token.equals("")) {
            Uri uri = Uri.parse("https://api.iijmio.jp/mobile/d/v1/authorization/?response_type=token&client_id=pZgayGOChl8Lm5ILZKy&state=0&redirect_uri=com.unk2072.iijmiotoggle%3A%2F%2Fcallback");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();
        if (action == null || !action.equals(Intent.ACTION_VIEW)) {
            Log.e(TAG, "onNewIntent getAction");
            return;
        }

        String data = intent.getDataString();
        if (data == null) {
            Log.e(TAG, "onNewIntent getDataString");
            return;
        }
        Log.i(TAG, "onNewIntent data=" + data);

        data = data.replace("#", "?");
        String token = Uri.parse(data).getQueryParameter("access_token");
        if (token == null) {
            Log.e(TAG, "onNewIntent getQueryParameter");
            return;
        }
        Log.i(TAG, "onNewIntent token=" + token);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(Const.ACCESS_TOKEN, token);
        edit.apply();

        boolean refresh_flag = pref.getBoolean(Const.REFRESH_FLAG, false);
        if (refresh_flag) {
            edit.putBoolean(Const.REFRESH_FLAG, false);
            edit.apply();
            String state = Uri.parse(data).getQueryParameter("state");
            Log.i(TAG, "onNewIntent state=" + state);
            int mode = Integer.valueOf(state);
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, mode);
            startService(i);
        }
    }

    private boolean initListView() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);
        int alarm_flag = pref.getInt(Const.ALARM_FLAG, 0);

        mListText[0] = run_flag ? getString(R.string.list1_1) : getString(R.string.list1_0);
        mListText[1] = getString(R.string.list2_0, getResources().getStringArray(R.array.select_array)[alarm_flag]);
        mListText[2] = getString(R.string.list3_0, pref.getInt(Const.OFF_HOUR, 8), pref.getInt(Const.OFF_MINUTE, 0));
        mListText[3] = getString(R.string.list4_0, pref.getInt(Const.ON_HOUR, 17), pref.getInt(Const.ON_MINUTE, 0));

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListText);
        ListView listView = (ListView) findViewById(R.id.listView);
        TextView textView = new TextView(this);
        textView.setText(R.string.list0_0);
        listView.addHeaderView(textView, null, false);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(this);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final String DIALOG = "dialog";
        switch (i) {
            case 0:
                break;
            case 1:
                doToggleService();
                break;
            case 2:
                new SettingDialog1().show(getFragmentManager(), DIALOG);
                break;
            case 3:
                new SettingDialog2().show(getFragmentManager(), DIALOG);
                break;
            case 4:
                new SettingDialog3().show(getFragmentManager(), DIALOG);
                break;
        }
    }

    private boolean doToggleService() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_STOP);
            startService(i);
            mListText[0] = getString(R.string.list1_0);
        } else {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_START);
            startService(i);
            mListText[0] = getString(R.string.list1_1);
        }
        mAdapter.notifyDataSetChanged();
        return true;
    }

    public static class SettingDialog1 extends DialogFragment implements DialogInterface.OnClickListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(R.array.select_array, this);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (3 < i) return;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(Const.ALARM_FLAG, i);
            edit.apply();

            MyActivity my = (MyActivity)getActivity();
            my.mListText[1] = getString(R.string.list2_0, getResources().getStringArray(R.array.select_array)[i]);
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog2 extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return new TimePickerDialog(getActivity(), this, pref.getInt(Const.OFF_HOUR, 8), pref.getInt(Const.OFF_MINUTE, 0), DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hour, int minute) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(Const.OFF_HOUR, hour);
            edit.putInt(Const.OFF_MINUTE, minute);
            edit.apply();

            MyActivity my = (MyActivity)getActivity();
            my.mListText[2] = getString(R.string.list3_0, hour, minute);
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog3 extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return new TimePickerDialog(getActivity(), this, pref.getInt(Const.ON_HOUR, 17), pref.getInt(Const.ON_MINUTE, 0), DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hour, int minute) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(Const.ON_HOUR, hour);
            edit.putInt(Const.ON_MINUTE, minute);
            edit.apply();

            MyActivity my = (MyActivity)getActivity();
            my.mListText[3] = getString(R.string.list4_0, hour, minute);
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    private boolean refreshSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_REFRESH);
            startService(i);
        }
        return true;
    }
}
