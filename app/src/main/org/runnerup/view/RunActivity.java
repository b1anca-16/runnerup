package org.runnerup.view;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;

import org.runnerup.R;
import org.runnerup.common.util.Constants;
import org.runnerup.tracker.LiveChallenge;
import org.runnerup.tracker.component.TrackerHRM;
import org.runnerup.util.Formatter;
import org.runnerup.util.ViewUtil;
import org.runnerup.workout.Intensity;
import org.runnerup.workout.Scope;
import org.runnerup.workout.Step;
import org.runnerup.workout.Workout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RunActivity extends BaseRunActivity {

  private Button pauseButton = null;
  private Button newLapButton = null;

  private TextView activityTime = null;
  private TextView activityDistance = null;
  private TextView activityPace = null;

  private TextView lapTime = null;
  private TextView lapDistance = null;
  private TextView lapPace = null;

  private TextView intervalTime = null;
  private TextView intervalDistance = null;
  private TextView intervalPace = null;

  private TextView currentPace = null;
  private TextView countdownView = null;

  private ListView workoutList = null;
  private View tableRowInterval = null;

  private Step currentStep = null;

  private TextView activityHr;
  private TextView lapHr;
  private TextView intervalHr;
  private TextView currentHr;
  private TextView activityHeaderHr;
  private TextView hrDebug;

  private final long[] mTapArray = {0, 0, 0, 0};
  private int mTapIndex = 0;

  // FIX #6: lastLocation entfernt – war toter Code (wurde gesetzt, aber nie genutzt)

  class WorkoutRow {
    Step step = null;
    ContentValues lap = null;
    public int level;
  }

  private final ArrayList<WorkoutRow> workoutRows = new ArrayList<>();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);

    if (!isLargeScreen()) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    setContentView(R.layout.run);

    formatter = new Formatter(this);

    TextView velocity = findViewById(R.id.velocity_label);
    velocity.setText(formatter.formatVelocityLabel());

    final Button stopButton = findViewById(R.id.stop_button);
    stopButton.setOnClickListener(stopButtonClick);

    pauseButton = findViewById(R.id.pause_button);
    pauseButton.setOnClickListener(pauseButtonClick);

    newLapButton = findViewById(R.id.new_lap_button);

    activityHeaderHr = findViewById(R.id.activity_header_hr);

    activityTime     = findViewById(R.id.run_activity_time);
    activityDistance = findViewById(R.id.run_activity_distance);
    activityPace     = findViewById(R.id.run_activity_pace);

    activityHr = findViewById(R.id.activity_hr);

    lapTime     = findViewById(R.id.lap_time);
    lapDistance = findViewById(R.id.lap_distance);
    lapPace     = findViewById(R.id.lap_pace);
    lapHr       = findViewById(R.id.lap_hr);

    intervalTime     = findViewById(R.id.run_interval_time);
    intervalDistance = findViewById(R.id.intervall_distance);
    tableRowInterval = findViewById(R.id.table_row_interval);
    intervalPace     = findViewById(R.id.interval_pace);
    intervalHr       = findViewById(R.id.interval_hr);

    currentPace = findViewById(R.id.current_pace);
    currentHr   = findViewById(R.id.current_hr);

    countdownView = findViewById(R.id.countdown_text_view);

    workoutList = findViewById(R.id.workout_list);
    hrDebug     = findViewById(R.id.hr_debug);

    WorkoutAdapter adapter = new WorkoutAdapter(workoutRows);
    workoutList.setAdapter(adapter);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    final Resources res = getResources();
    final boolean active = prefs.getBoolean(res.getString(R.string.pref_lock_run), false);

    if (!prefs.getBoolean(res.getString(R.string.pref_bt_debug), false)) {
      hrDebug = null;
    }

    TableLayout tableLayout = findViewById(R.id.table_layout1);
    tableLayout.setOnTouchListener(
            (v, event) -> {
              int action = event.getAction();

              if (active && action == MotionEvent.ACTION_DOWN) {
                final int maxTapTime = 1000;
                long time = event.getEventTime();

                if (mTapArray[mTapIndex] != 0
                        && time - mTapArray[mTapIndex] < maxTapTime) {
                  boolean enabled = !pauseButton.isEnabled();

                  pauseButton.setEnabled(enabled);
                  stopButton.setEnabled(enabled);

                  Arrays.fill(mTapArray, 0);
                } else {
                  if (mTapIndex == 0) {
                    Toast.makeText(
                            getApplicationContext(),
                            res.getString(
                                    org.runnerup.common.R.string.Lock_activity_buttons_message
                            ),
                            Toast.LENGTH_SHORT
                    ).show();
                  }

                  mTapArray[mTapIndex] = time;
                  mTapIndex = (mTapIndex + 1) % mTapArray.length;
                }
              }

              return false;
            }
    );

    initRunSession();

    getOnBackPressedDispatcher()
            .addCallback(
                    this,
                    new OnBackPressedCallback(true) {
                      @Override
                      public void handleOnBackPressed() {
                        // Zurück-Taste während einer Aktivität ignorieren
                      }
                    }
            );

    ViewUtil.Insets(findViewById(R.id.start_view), true);
  }

  private boolean isLargeScreen() {
    int screenSize =
            getResources().getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK;

    return screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE;
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Log.d(getClass().getName(), "onConfigurationChange => do NOTHING!!");
  }

  @Override
  protected void onWorkoutReady() {
    HashMap<String, Object> bindValues = new HashMap<>();
    bindValues.put(Workout.KEY_COUNTER_VIEW, countdownView);
    workout.onBind(workout, bindValues);

    populateWorkoutList();

    newLapButton.setOnClickListener(newLapButtonClick);
    newLapButton.setText(org.runnerup.common.R.string.Lap);
  }

  @Override
  protected void onRunTick() {
    double km   = workout.getDistance(Scope.ACTIVITY);
    double pace = workout.getSpeed(Scope.ACTIVITY);
    double time = workout.getTime(Scope.ACTIVITY);

    LiveChallenge.getInstance().sendUpdate(km);
  }

  @Override
  protected void onRunDataUpdated() {
    updateView();
  }

  @Override
  protected void onPauseStateChanged(boolean paused) {
    setPauseButtonEnabled(!paused);
  }

  private void populateWorkoutList() {
    List<Workout.StepListEntry> list = workout.getStepList();

    for (Workout.StepListEntry entry : list) {
      WorkoutRow row = new WorkoutRow();
      row.level = entry.level();
      row.step  = entry.step();
      row.lap   = null;

      workoutRows.add(row);
    }
  }

  private final OnClickListener stopButtonClick  = v -> stopCurrentRun();
  private final OnClickListener pauseButtonClick = v -> togglePauseState();

  private final OnClickListener newLapButtonClick =
          v -> {
            if (workout != null) {
              workout.onNewLapOrNextStep();
            }
          };

  private void setPauseButtonEnabled(boolean enabled) {
    if (enabled) {
      pauseButton.setText(org.runnerup.common.R.string.Pause);

      ViewCompat.setBackground(
              pauseButton,
              AppCompatResources.getDrawable(this, R.drawable.btn_blue)
      );

      pauseButton.setCompoundDrawablesWithIntrinsicBounds(
              0,
              0,
              org.runnerup.common.R.drawable.ic_av_pause,
              0
      );
    } else {
      pauseButton.setText(org.runnerup.common.R.string.Resume);

      ViewCompat.setBackground(
              pauseButton,
              AppCompatResources.getDrawable(this, R.drawable.btn_green)
      );

      pauseButton.setCompoundDrawablesWithIntrinsicBounds(
              0,
              0,
              org.runnerup.common.R.drawable.ic_av_play_arrow,
              0
      );
    }
  }

  private void updateView() {
    if (workout == null || mTracker == null) {
      return;
    }

    setPauseButtonEnabled(!workout.isPaused());

    double activityDistanceValue = workout.getDistance(Scope.ACTIVITY);
    double activityTimeValue     = workout.getTime(Scope.ACTIVITY);
    double activityPaceValue     = workout.getSpeed(Scope.ACTIVITY);

    activityTime.setText(
            formatter.formatElapsedTime(
                    Formatter.Format.TXT_SHORT,
                    Math.round(activityTimeValue)
            )
    );

    activityDistance.setText(
            formatter.formatDistance(
                    Formatter.Format.TXT_SHORT,
                    Math.round(activityDistanceValue)
            )
    );

    activityPace.setText(
            formatter.formatVelocityByPreferredUnit(
                    Formatter.Format.TXT_SHORT,
                    activityPaceValue
            )
    );

    double lapDistanceValue = workout.getDistance(Scope.LAP);
    double lapTimeValue     = workout.getTime(Scope.LAP);
    double lapPaceValue     = workout.getSpeed(Scope.LAP);

    lapTime.setText(
            formatter.formatElapsedTime(
                    Formatter.Format.TXT_SHORT,
                    Math.round(lapTimeValue)
            )
    );

    lapDistance.setText(
            formatter.formatDistance(
                    Formatter.Format.TXT_LONG,
                    Math.round(lapDistanceValue)
            )
    );

    lapPace.setText(
            formatter.formatVelocityByPreferredUnit(
                    Formatter.Format.TXT_SHORT,
                    lapPaceValue
            )
    );

    if (tableRowInterval != null) {
      if (currentStep != null
              && workout.getWorkoutType() != Constants.WORKOUT_TYPE.BASIC
              && currentStep.getIntensity() == Intensity.ACTIVE) {

        double intervalDistanceValue = workout.getDistance(Scope.STEP);
        double intervalTimeValue     = workout.getTime(Scope.STEP);
        double intervalPaceValue     = workout.getSpeed(Scope.STEP);

        tableRowInterval.setVisibility(View.VISIBLE);

        intervalTime.setText(
                formatter.formatElapsedTime(
                        Formatter.Format.TXT_SHORT,
                        Math.round(intervalTimeValue)
                )
        );

        intervalDistance.setText(
                formatter.formatDistance(
                        Formatter.Format.TXT_LONG,
                        Math.round(intervalDistanceValue)
                )
        );

        intervalPace.setText(
                formatter.formatVelocityByPreferredUnit(
                        Formatter.Format.TXT_SHORT,
                        intervalPaceValue
                )
        );
      } else {
        tableRowInterval.setVisibility(View.GONE);
      }
    }

    double currentPaceValue = workout.getSpeed(Scope.CURRENT);

    currentPace.setText(
            formatter.formatVelocityByPreferredUnit(
                    Formatter.Format.TXT_SHORT,
                    currentPaceValue
            )
    );

    if (mTracker.isComponentConnected(TrackerHRM.NAME)) {
      double activityHeartRate = workout.getHeartRate(Scope.ACTIVITY);
      double intervalHeartRate = workout.getHeartRate(Scope.STEP);
      double lapHeartRate      = workout.getHeartRate(Scope.LAP);
      double currentHeartRate  = workout.getHeartRate(Scope.CURRENT);

      lapHr.setText(formatter.formatHeartRate(Formatter.Format.TXT_SHORT, lapHeartRate));
      intervalHr.setText(formatter.formatHeartRate(Formatter.Format.TXT_SHORT, intervalHeartRate));
      currentHr.setText(formatter.formatHeartRate(Formatter.Format.TXT_SHORT, currentHeartRate));
      activityHr.setText(formatter.formatHeartRate(Formatter.Format.TXT_SHORT, activityHeartRate));

      activityHr.setVisibility(View.VISIBLE);
      lapHr.setVisibility(View.VISIBLE);
      intervalHr.setVisibility(View.VISIBLE);
      currentHr.setVisibility(View.VISIBLE);
      activityHeaderHr.setVisibility(View.VISIBLE);

      if (hrDebug != null) {
        hrDebug.setVisibility(View.VISIBLE);
        mTracker.setHrDebug(hrDebug);
      }
    } else {
      activityHr.setVisibility(View.GONE);
      lapHr.setVisibility(View.GONE);
      intervalHr.setVisibility(View.GONE);
      currentHr.setVisibility(View.GONE);
      activityHeaderHr.setVisibility(View.GONE);
    }

    Step currentWorkoutStep = workout.getCurrentStep();

    if (currentWorkoutStep != currentStep) {
      ((WorkoutAdapter) workoutList.getAdapter()).notifyDataSetChanged();

      currentStep = currentWorkoutStep;
      workoutList.setSelection(getPosition(workoutRows, currentStep));
    }
  }

  private int getPosition(ArrayList<WorkoutRow> rows, Step currentActivity) {
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i).step == currentActivity) {
        return i;
      }
    }

    return 0;
  }

  class WorkoutAdapter extends BaseAdapter {

    final ArrayList<WorkoutRow> rows;

    WorkoutAdapter(ArrayList<WorkoutRow> workoutRows) {
      this.rows = workoutRows;
    }

    @Override
    public int getCount() {
      return rows.size();
    }

    @Override
    public Object getItem(int position) {
      return rows.get(position);
    }

    // FIX #8: Position als stabile ID zurückgeben statt immer 0
    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      WorkoutRow row = rows.get(position);

      if (row.step != null) {
        return getWorkoutRow(row.step, row.level, convertView, parent);
      }

      return getLapRow(row.lap, convertView, parent);
    }

    private View getWorkoutRow(
            Step step,
            int level,
            View convertView,
            ViewGroup parent
    ) {
      // FIX #7: convertView wiederverwenden um unnötiges Inflating zu vermeiden
      View view = convertView;

      if (view == null || view.findViewById(R.id.workout_step_intensity) == null) {
        LayoutInflater inflater = LayoutInflater.from(RunActivity.this);
        view = inflater.inflate(R.layout.workout_row, parent, false);
      }

      TextView intensity     = view.findViewById(R.id.workout_step_intensity);
      TextView durationType  = view.findViewById(R.id.workout_step_duration_type);
      TextView durationValue = view.findViewById(R.id.workout_step_duration_value);
      TextView targetPace    = view.findViewById(R.id.workout_step_pace);

      intensity.setPadding(level * 10, 0, 0, 0);
      intensity.setText(getResources().getText(step.getIntensity().getTextId()));

      if (step.getDurationType() != null) {
        durationType.setText(getResources().getText(step.getDurationType().getTextId()));

        durationValue.setText(
                formatter.format(
                        Formatter.Format.TXT_LONG,
                        step.getDurationType(),
                        step.getDurationValue()
                )
        );
      } else {
        durationType.setText("");
        durationValue.setText("");
      }

      if (currentStep != step) {
        view.setBackgroundResource(android.R.color.black);
      } else {
        view.setBackgroundResource(android.R.color.transparent);
      }

      if (step.getTargetType() == null) {
        targetPace.setText("");
      } else {
        double minValue = step.getTargetValue().minValue;
        double maxValue = step.getTargetValue().maxValue;

        if (minValue == maxValue) {
          targetPace.setText(
                  formatter.format(
                          Formatter.Format.TXT_SHORT,
                          step.getTargetType(),
                          minValue
                  )
          );
        } else {
          targetPace.setText(
                  String.format(
                          Locale.getDefault(),
                          "%s-%s",
                          formatter.format(
                                  Formatter.Format.TXT_SHORT,
                                  step.getTargetType(),
                                  minValue
                          ),
                          formatter.format(
                                  Formatter.Format.TXT_SHORT,
                                  step.getTargetType(),
                                  maxValue
                          )
                  )
          );
        }
      }

      if (step.getIntensity() == Intensity.REPEAT) {
        if (step.getCurrentRepeat() >= step.getRepeatCount()) {
          durationValue.setText(org.runnerup.common.R.string.Finished);
        } else {
          durationValue.setText(
                  String.format(
                          Locale.getDefault(),
                          "%d/%d",
                          step.getCurrentRepeat() + 1,
                          step.getRepeatCount()
                  )
          );
        }
      }

      return view;
    }

    private View getLapRow(ContentValues lap, View convertView, ViewGroup parent) {
      // convertView für LapRows ebenfalls wiederverwenden
      if (convertView != null && convertView.findViewById(R.id.workout_step_intensity) == null) {
        return convertView;
      }

      LayoutInflater inflater = LayoutInflater.from(RunActivity.this);
      return inflater.inflate(R.layout.laplist_row, parent, false);
    }
  }
}