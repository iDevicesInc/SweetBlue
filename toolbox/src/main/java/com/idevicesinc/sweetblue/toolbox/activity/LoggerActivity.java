package com.idevicesinc.sweetblue.toolbox.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.idevicesinc.sweetblue.toolbox.util.DebugLog;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.view.ReselectableSpinner;
import com.idevicesinc.sweetblue.utils.DebugLogger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static com.idevicesinc.sweetblue.utils.Utils.isOnMainThread;

public class LoggerActivity extends BaseActivity implements DebugLogger.LogEvent
{

    private ReselectableSpinner m_debugSpinner;
    private TextView m_logTextView;
    private EditText m_filterEditText;
    private Button m_button;

    private int m_logLevel;
    private String m_filter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.logger_title));

        DebugLog.getDebugger().setLogListener(this);

        m_logTextView = find(R.id.logTextView);
        m_debugSpinner = find(R.id.debugSpinner);
        m_filterEditText = find(R.id.filterEditText);
        m_button = find(R.id.button);

        m_debugSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                m_logLevel = position + 2;
                refreshLog();
            }

            @Override public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        m_button.setOnClickListener(new OnClickListener()
        {
            @Override public void onClick(View v)
            {
                m_filter = m_filterEditText.getText().toString();
                refreshLog();
            }
        });
    }

    private void refreshLog(){

        StringBuilder logString = new StringBuilder();
        List<String> log = DebugLog.getDebugger().getLogList();
        String level = DebugLogger.level(m_logLevel);
        Pattern filterPattern = Pattern.compile(m_filter);

        for(int i=0; i<log.size(); i++){
            String entry = log.get(i).substring(11,20) + log.get(i).substring(29);
            Matcher entryMatcher = filterPattern.matcher(entry);

            if (level.equals("Verbose")){
                if(m_filter.equals("") || entryMatcher.find()) {
                    logString.append(entry + "\n\n");
                }
            }
            else if (entry.contains(level)){
                if(m_filter.equals("") || entryMatcher.find()) {
                    logString.append(entry + "\n\n");
                }
            }
        }
        m_logTextView.setText(logString);
    }

    private void writeNewLine(String entry){

        String level = DebugLogger.level(m_logLevel);
        Pattern filterPattern = Pattern.compile(m_filter);

        entry = entry.substring(11,20) + entry.substring(29);
        Matcher entryMatcher = filterPattern.matcher(entry);
        if (level.equals("Verbose")){
            if(m_filter.equals("") || entryMatcher.find()) {
                m_logTextView.append(entry + "\n\n");
            }
        }
        else if (entry.contains(level)){
            if(m_filter.equals("") || entryMatcher.find()) {
                m_logTextView.append(entry + "\n\n");
            }
        }
    }

    @Override
    public boolean onNavigateUp()
    {
        DebugLog.getDebugger().setLogListener(null);
        finish();
        return true;
    }

    @Override public void onLogEntry(final String entry)
    {
        if (isOnMainThread())
            writeNewLine(entry);
        else
            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run()
                {
                    writeNewLine(entry);
                }
            });
    }
}
