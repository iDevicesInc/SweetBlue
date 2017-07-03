package com.idevicesinc.sweetblue.toolbox.activity;


import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.toolbox.util.DebugLog;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.utils.DebugLogger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.idevicesinc.sweetblue.utils.Utils.isOnMainThread;


public class LoggerActivity extends BaseActivity implements DebugLogger.LogEvent
{

    private SwipeRefreshLayout mSwipeLayout;
    private TextView m_logTextView;

    private int m_logLevel = 2;
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

        ImageView logo = find(R.id.navBarLogo);
        logo.setVisibility(View.GONE);

        mSwipeLayout = find(R.id.swipeLayout);

        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                refreshLog();
            }
        });

        DebugLog.getDebugger().setLogListener(this);

        m_logTextView = find(R.id.logTextView);

        refreshLog();

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

        mSwipeLayout.setRefreshing(false);
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
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        getMenuInflater().inflate(R.menu.logger, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.filter));
        if (searchView != null)
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    m_filter = query;
                    refreshLog();
                    MenuItemCompat.collapseActionView(menu.findItem(R.id.filter));
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    m_filter = newText;
                    refreshLog();
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.filter)
        {
            return true;
        }
        else
        {
            item.setChecked(true);
            switch (item.getItemId())
            {
                case R.id.verbose:
                    m_logLevel = 2;
                    break;
                case R.id.debug:
                    m_logLevel = 3;
                    break;
                case R.id.info:
                    m_logLevel = 4;
                    break;
                case R.id.warn:
                    m_logLevel = 5;
                    break;
                case R.id.error:
                    m_logLevel = 6;
                    break;
                default:
                    return super.onOptionsItemSelected(item);
            }
            refreshLog();
            return true;
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
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    writeNewLine(entry);
                }
            });
    }
}
