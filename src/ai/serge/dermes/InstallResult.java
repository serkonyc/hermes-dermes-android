package ai.serge.dermes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/**
 * Result callback for in-app self-update staged via PackageInstaller.
 * STATUS_PENDING_USER_ACTION carries the actual install sheet as an intent
 * extra - launching it is the whole point (ACTION_VIEW handoff is what
 * One UI 15 used to reject with "can't install app").
 */
public class InstallResult extends BroadcastReceiver {
    @Override @SuppressWarnings("unchecked")
    public void onReceive(Context c, Intent i) {
        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent sheet = i.getParcelableExtra(Intent.EXTRA_INTENT);
            if (sheet != null) {
                sheet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(sheet);
            }
        } else if (st != PackageInstaller.STATUS_SUCCESS) {
            String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(c, "install failed: " + msg, Toast.LENGTH_LONG).show();
        }
    }
}
