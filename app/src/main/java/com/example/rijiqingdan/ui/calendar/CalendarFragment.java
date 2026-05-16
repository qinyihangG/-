package com.example.rijiqingdan.ui.calendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rijiqingdan.MainActivity;
import com.example.rijiqingdan.R;
import com.example.rijiqingdan.data.AppDatabase;
import com.example.rijiqingdan.data.Task;
import com.example.rijiqingdan.data.TaskDao;
import com.example.rijiqingdan.ui.notes.TaskAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private CalendarView calendarView;
    private TextView selectedDateView;
    private TextView markerView;
    private TextView emptyHint;
    private Button addButton;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;

    private String selectedDate;
    private TaskDao dao;
    private LiveData<List<Task>> currentTasks;
    private LiveData<List<String>> allDatesLive;
    private List<String> allDates = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = AppDatabase.getDatabase(requireContext()).taskDao();
        selectedDate = DATE_FMT.format(Calendar.getInstance().getTime());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = root.findViewById(R.id.calendar_view);
        selectedDateView = root.findViewById(R.id.tv_selected_date);
        markerView = root.findViewById(R.id.tv_marker);
        emptyHint = root.findViewById(R.id.tv_empty);
        addButton = root.findViewById(R.id.btn_add);
        recyclerView = root.findViewById(R.id.rv_tasks);

        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onEdit(Task task) {
                showEditDialog(task);
            }

            @Override
            public void onDelete(Task task) {
                showDeleteConfirm(task);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        selectedDateView.setText(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day);
            selectedDate = DATE_FMT.format(c.getTime());
            selectedDateView.setText(selectedDate);
            observeTasks();
        });

        addButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openNotesForDate(selectedDate);
            }
        });

        observeTasks();
        observeAllDates();
        return root;
    }

    private void observeTasks() {
        if (currentTasks != null) {
            currentTasks.removeObservers(getViewLifecycleOwner());
        }
        currentTasks = dao.getTasksByDate(selectedDate);
        currentTasks.observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTasks(tasks);
            int count = tasks == null ? 0 : tasks.size();
            selectedDateView.setText(getString(R.string.selected_date_with_count, selectedDate, count));
            emptyHint.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        });
    }

    private void observeAllDates() {
        allDatesLive = dao.getAllDates();
        allDatesLive.observe(getViewLifecycleOwner(), dates -> {
            allDates = dates != null ? dates : new ArrayList<>();
            updateMarkerText();
        });
    }

    private void updateMarkerText() {
        if (allDates.isEmpty()) {
            markerView.setText(R.string.no_task_dates);
            return;
        }
        String currentMonth = selectedDate.substring(0, 7);
        List<String> inMonth = new ArrayList<>();
        for (String d : allDates) {
            if (d != null && d.startsWith(currentMonth)) {
                inMonth.add(d.substring(8));
            }
        }
        if (inMonth.isEmpty()) {
            markerView.setText(R.string.no_task_this_month);
        } else {
            markerView.setText(getString(R.string.task_dates_format, TextUtils.join(", ", inMonth)));
        }
    }

    private void showEditDialog(Task task) {
        final EditText et = new EditText(requireContext());
        et.setText(task.content);
        et.setSelection(task.content == null ? 0 : task.content.length());

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_edit_title)
                .setView(et)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String newContent = et.getText().toString().trim();
                    if (TextUtils.isEmpty(newContent)) {
                        Toast.makeText(requireContext(), R.string.toast_empty_content, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    task.content = newContent;
                    task.updatedAt = System.currentTimeMillis();
                    AppDatabase.databaseWriteExecutor.execute(() -> dao.update(task));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showDeleteConfirm(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.action_delete,
                        (d, w) -> AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(task)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
