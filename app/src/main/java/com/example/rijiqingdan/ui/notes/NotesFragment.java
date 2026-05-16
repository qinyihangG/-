package com.example.rijiqingdan.ui.notes;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.example.rijiqingdan.R;
import com.example.rijiqingdan.data.AppDatabase;
import com.example.rijiqingdan.data.Task;
import com.example.rijiqingdan.data.TaskDao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotesFragment extends Fragment {

    private static final String ARG_DATE = "arg_date";
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private TextView dateTextView;
    private EditText contentInput;
    private Button saveButton;
    private TextView emptyHint;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;

    private String selectedDate;
    private TaskDao dao;
    private LiveData<List<Task>> currentTasks;

    public static NotesFragment newInstance(@Nullable String date) {
        NotesFragment f = new NotesFragment();
        Bundle args = new Bundle();
        if (date != null) args.putString(ARG_DATE, date);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = AppDatabase.getDatabase(requireContext()).taskDao();

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_DATE)) {
            selectedDate = args.getString(ARG_DATE);
        } else {
            selectedDate = DATE_FMT.format(Calendar.getInstance().getTime());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notes, container, false);

        dateTextView = root.findViewById(R.id.tv_date);
        contentInput = root.findViewById(R.id.et_content);
        saveButton = root.findViewById(R.id.btn_save);
        emptyHint = root.findViewById(R.id.tv_empty);
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

        dateTextView.setText(selectedDate);
        dateTextView.setOnClickListener(v -> pickDate());

        saveButton.setOnClickListener(v -> saveNewTask());

        observeTasks();
        return root;
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(DATE_FMT.parse(selectedDate));
        } catch (Exception ignored) { }

        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, day);
                    selectedDate = DATE_FMT.format(picked.getTime());
                    dateTextView.setText(selectedDate);
                    observeTasks();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveNewTask() {
        String content = contentInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(requireContext(), R.string.toast_empty_content, Toast.LENGTH_SHORT).show();
            return;
        }
        Task task = new Task();
        task.date = selectedDate;
        task.content = content;
        long now = System.currentTimeMillis();
        task.createdAt = now;
        task.updatedAt = now;
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(task));
        contentInput.setText("");
        Toast.makeText(requireContext(), R.string.toast_saved, Toast.LENGTH_SHORT).show();
    }

    private void observeTasks() {
        if (currentTasks != null) {
            currentTasks.removeObservers(getViewLifecycleOwner());
        }
        currentTasks = dao.getTasksByDate(selectedDate);
        currentTasks.observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTasks(tasks);
            emptyHint.setVisibility(
                    (tasks == null || tasks.isEmpty()) ? View.VISIBLE : View.GONE);
        });
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
