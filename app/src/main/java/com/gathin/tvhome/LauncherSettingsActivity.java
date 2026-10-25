package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private static final int[] DOCK_PADDINGS = {6, 10, 16, 22};
    private final TextView[] dockPaddingChoices = new TextView[DOCK_PADDINGS.length + 1];
    private static final int[] DOCK_GAPS = {16, 24, 32, 40};
    private final TextView[] dockGapChoices = new TextView[DOCK_GAPS.length + 1];
    private int dockPaddingVertical;
    private int dockGapBelow;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    private interface IntConsumer {
        void accept(int value);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        dockPaddingVertical = config.dockPaddingVertical;
        dockGapBelow = config.dockGapBelow;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] padIds = {R.id.dockPad6, R.id.dockPad10, R.id.dockPad16, R.id.dockPad22, R.id.dockPadCustom};
        for (int i = 0; i < padIds.length; i++) {
            dockPaddingChoices[i] = findViewById(padIds[i]);
            if (i < DOCK_PADDINGS.length) {
                final int value = DOCK_PADDINGS[i];
                dockPaddingChoices[i].setOnClickListener(v -> {
                    if (dockPaddingVertical == value) return;
                    dockPaddingVertical = value;
                    int hPad = Math.round(value * 1.6f);
                    choices.edit().putInt("dock_padding_vertical", value)
                            .putInt("dock_padding_horizontal", hPad).apply();
                    changed();
                    refresh();
                });
            } else {
                dockPaddingChoices[i].setOnClickListener(v -> {
                    showCustomNumberDialog(getString(R.string.dock_size_label), dockPaddingVertical, 0, 80, val -> {
                        dockPaddingVertical = val;
                        int hPad = Math.round(val * 1.6f);
                        choices.edit().putInt("dock_padding_vertical", val)
                                .putInt("dock_padding_horizontal", hPad).apply();
                        changed();
                        refresh();
                    });
                });
            }
        }
        int[] gapIds = {R.id.dockGap16, R.id.dockGap24, R.id.dockGap32, R.id.dockGap40, R.id.dockGapCustom};
        for (int i = 0; i < gapIds.length; i++) {
            dockGapChoices[i] = findViewById(gapIds[i]);
            if (i < DOCK_GAPS.length) {
                final int value = DOCK_GAPS[i];
                dockGapChoices[i].setOnClickListener(v -> {
                    if (dockGapBelow == value) return;
                    dockGapBelow = value;
                    choices.edit().putInt("dock_gap_below", value).apply();
                    changed();
                    refresh();
                });
            } else {
                dockGapChoices[i].setOnClickListener(v -> {
                    showCustomNumberDialog(getString(R.string.dock_gap_label), dockGapBelow, 0, 100, val -> {
                        dockGapBelow = val;
                        choices.edit().putInt("dock_gap_below", val).apply();
                        changed();
                        refresh();
                    });
                });
            }
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
            reverseRow(dockPaddingChoices);
            reverseRow(dockGapChoices);
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showCustomNumberDialog(String title, int initialValue, int min, int max, IntConsumer onValue) {
        final int[] currentVal = {initialValue};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        int btnPad = (int) (12 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);

        TextView btnMinus10 = new TextView(this);
        btnMinus10.setText("-10");
        btnMinus10.setTextSize(16);
        btnMinus10.setTextColor(0xFFFFFFFF);
        btnMinus10.setBackgroundResource(R.drawable.settings_choice_bg);
        btnMinus10.setFocusable(true);
        btnMinus10.setGravity(android.view.Gravity.CENTER);
        btnMinus10.setPadding(btnPad, btnPad / 2, btnPad, btnPad / 2);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.setMarginEnd(margin);
        btnMinus10.setLayoutParams(lp1);

        TextView btnMinus2 = new TextView(this);
        btnMinus2.setText("-2");
        btnMinus2.setTextSize(16);
        btnMinus2.setTextColor(0xFFFFFFFF);
        btnMinus2.setBackgroundResource(R.drawable.settings_choice_bg);
        btnMinus2.setFocusable(true);
        btnMinus2.setGravity(android.view.Gravity.CENTER);
        btnMinus2.setPadding(btnPad, btnPad / 2, btnPad, btnPad / 2);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMarginEnd(margin);
        btnMinus2.setLayoutParams(lp2);

        final android.widget.EditText valEdit = new android.widget.EditText(this);
        valEdit.setText(String.valueOf(currentVal[0]));
        valEdit.setTextSize(22);
        valEdit.setTextColor(0xFFFFFFFF);
        valEdit.setGravity(android.view.Gravity.CENTER);
        valEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                (int) (90 * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editLp.setMarginEnd(margin / 2);
        valEdit.setLayoutParams(editLp);

        TextView unitText = new TextView(this);
        unitText.setText("dp");
        unitText.setTextSize(18);
        unitText.setTextColor(0xFFACB8C1);
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        unitLp.setMarginEnd(margin);
        unitText.setLayoutParams(unitLp);

        TextView btnPlus2 = new TextView(this);
        btnPlus2.setText("+2");
        btnPlus2.setTextSize(16);
        btnPlus2.setTextColor(0xFFFFFFFF);
        btnPlus2.setBackgroundResource(R.drawable.settings_choice_bg);
        btnPlus2.setFocusable(true);
        btnPlus2.setGravity(android.view.Gravity.CENTER);
        btnPlus2.setPadding(btnPad, btnPad / 2, btnPad, btnPad / 2);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.setMarginEnd(margin);
        btnPlus2.setLayoutParams(lp3);

        TextView btnPlus10 = new TextView(this);
        btnPlus10.setText("+10");
        btnPlus10.setTextSize(16);
        btnPlus10.setTextColor(0xFFFFFFFF);
        btnPlus10.setBackgroundResource(R.drawable.settings_choice_bg);
        btnPlus10.setFocusable(true);
        btnPlus10.setGravity(android.view.Gravity.CENTER);
        btnPlus10.setPadding(btnPad, btnPad / 2, btnPad, btnPad / 2);

        btnMinus10.setOnClickListener(v -> {
            try { currentVal[0] = Integer.parseInt(valEdit.getText().toString().trim()); } catch (Exception ignored) {}
            currentVal[0] = Math.max(min, currentVal[0] - 10);
            valEdit.setText(String.valueOf(currentVal[0]));
        });
        btnMinus2.setOnClickListener(v -> {
            try { currentVal[0] = Integer.parseInt(valEdit.getText().toString().trim()); } catch (Exception ignored) {}
            currentVal[0] = Math.max(min, currentVal[0] - 2);
            valEdit.setText(String.valueOf(currentVal[0]));
        });
        btnPlus2.setOnClickListener(v -> {
            try { currentVal[0] = Integer.parseInt(valEdit.getText().toString().trim()); } catch (Exception ignored) {}
            currentVal[0] = Math.min(max, currentVal[0] + 2);
            valEdit.setText(String.valueOf(currentVal[0]));
        });
        btnPlus10.setOnClickListener(v -> {
            try { currentVal[0] = Integer.parseInt(valEdit.getText().toString().trim()); } catch (Exception ignored) {}
            currentVal[0] = Math.min(max, currentVal[0] + 10);
            valEdit.setText(String.valueOf(currentVal[0]));
        });

        row.addView(btnMinus10);
        row.addView(btnMinus2);
        row.addView(valEdit);
        row.addView(unitText);
        row.addView(btnPlus2);
        row.addView(btnPlus10);
        layout.addView(row);

        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            int finalVal = currentVal[0];
            try {
                finalVal = Integer.parseInt(valEdit.getText().toString().trim());
            } catch (Exception ignored) {}
            finalVal = Math.max(min, Math.min(max, finalVal));
            onValue.accept(finalVal);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        btnPlus2.requestFocus();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        boolean customPad = true;
        for (int i = 0; i < DOCK_PADDINGS.length; i++) {
            boolean match = (dockPaddingVertical == DOCK_PADDINGS[i]);
            dockPaddingChoices[i].setSelected(match);
            if (match) customPad = false;
        }
        dockPaddingChoices[DOCK_PADDINGS.length].setSelected(customPad);
        dockPaddingChoices[DOCK_PADDINGS.length].setText(customPad ? (dockPaddingVertical + "dp") : "Custom...");

        boolean customGap = true;
        for (int i = 0; i < DOCK_GAPS.length; i++) {
            boolean match = (dockGapBelow == DOCK_GAPS[i]);
            dockGapChoices[i].setSelected(match);
            if (match) customGap = false;
        }
        dockGapChoices[DOCK_GAPS.length].setSelected(customGap);
        dockGapChoices[DOCK_GAPS.length].setText(customGap ? (dockGapBelow + "dp") : "Custom...");

        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
    }
}
