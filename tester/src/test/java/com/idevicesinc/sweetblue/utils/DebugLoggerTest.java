package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.BaseTest;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class DebugLoggerTest extends BaseTest
{

    @Test
    public void getLogListTest() throws Exception
    {
        startTest(false);
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        List<String> logList = log.getLogList();
        assertTrue(logList.size() == 10);
        assertTrue(logList.get(9).endsWith("12"));
        succeed();
    }

    @Test
    public void getLastLogsTest() throws Exception
    {
        startTest(false);
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        List<String> list = log.getLastLogs(5);
        assertTrue(list.size() == 5);
        assertTrue(list.get(4).endsWith("12"));
        succeed();
    }

    @Test
    public void getLastLogTest() throws Exception
    {
        startTest(false);
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        String last = log.getLastLog();
        assertNotNull(last);
        assertTrue(last.endsWith("12"));
        succeed();
    }
}
