package com.example.rijiqingdan.ui.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rijiqingdan.R;
import com.example.rijiqingdan.data.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 时间线适配器：把任务按日期分组渲染，每个日期一个 header，下面接当天任务。
 * 列表按日期升序，确保「今天」总是有 header（即使当天还没任务）。
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK = 1;

    private static final SimpleDateFormat IN_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat OUT_FMT =
            new SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE);

    public interface OnTaskActionListener {
        void onEdit(Task task);
        void onDelete(Task task);
    }

    private final OnTaskActionListener listener;
    private final String todayDate;
    /** Items 是 String (date header) 或 Task 的混合列表 */
    private final List<Object> items = new ArrayList<>();

    public TimelineAdapter(OnTaskActionListener listener, String todayDate) {
        this.listener = listener;
        this.todayDate = todayDate;
    }

    public void setTasks(List<Task> tasks) {
        items.clear();
        if (tasks == null) tasks = new ArrayList<>();

        Set<String> seenDates = new HashSet<>();
        String lastDate = null;
        for (Task t : tasks) {
            if (t.date == null) continue;
            if (!t.date.equals(lastDate)) {
                items.add(t.date);
                lastDate = t.date;
                seenDates.add(t.date);
            }
            items.add(t);
        }

        // 即使今天没有任务，也插入今天的 header 占位
        if (!seenDates.contains(todayDate)) {
            int insertPos = items.size();
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                if (item instanceof String && ((String) item).compareTo(todayDate) > 0) {
                    insertPos = i;
                    break;
                }
            }
            items.add(insertPos, todayDate);
        }

        notifyDataSetChanged();
    }

    /** 返回今天 header 在列表中的位置，找不到返回 -1 */
    public int findTodayPosition() {
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof String && todayDate.equals(item)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inf.inflate(R.layout.item_date_header, parent, false));
        }
        return new TaskVH(inf.inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((String) item);
        } else if (holder instanceof TaskVH) {
            ((TaskVH) holder).bind((Task) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatHeader(String dateStr) {
        try {
            Date d = IN_FMT.parse(dateStr);
            String formatted = OUT_FMT.format(d);
            if (dateStr.equals(todayDate)) {
                formatted += "（今天）";
            }
            return formatted;
        } catch (Exception e) {
            return dateStr;
        }
    }

    class HeaderVH extends RecyclerView.ViewHolder {
        TextView tv;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_date_header);
        }
        void bind(String date) {
            tv.setText(formatHeader(date));
        }
    }

    class TaskVH extends RecyclerView.ViewHolder {
        TextView content;
        ImageButton edit;
        ImageButton delete;
        TaskVH(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.task_content);
            edit = itemView.findViewById(R.id.btn_edit);
            delete = itemView.findViewById(R.id.btn_delete);
        }
        void bind(Task task) {
            content.setText(task.content);
            edit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(task);
            });
            delete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(task);
            });
        }
    }
}
