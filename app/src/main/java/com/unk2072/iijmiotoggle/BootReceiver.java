package com.unk2072.iijmiotoggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String RUN_FLAG = "run_flag";
    private static final String RUN_MODE = "run_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) return;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean run_flag = pref.getBoolean(RUN_FLAG, false);
        if (run_flag) {
            Intent i = new Intent(context, MyService.class);
            i.putExtra(RUN_MODE, 0);
            context.startService(i);
        }
    }
}
