package com.idevicesinc.gattspecparser;

import android.os.AsyncTask;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLRipper
{
    public interface onFinishListener{
        void onFinish(List<Map<String, String>> l);
    }

    public XMLRipper(final String baseURL, final Pattern baseURLPattern, final onFinishListener listener)
    {
        AsyncTask<String[], Void, Void> async = new AsyncTask<String[], Void, Void>()
        {
            @Override
            protected Void doInBackground(String[]... params)
            {
                System.out.println("++-- Starting async task");

                List<Map<String, String>> l = new ArrayList<>();

                //FIXME:  Figure out how to pass in array
                ArrayList<String> xmlURLs = new ArrayList<>();
                try
                {
                    String basePageHTML = downloadURL(baseURL);
                    Matcher m = baseURLPattern.matcher(basePageHTML);
                    while (m.find()){
                        xmlURLs.add("https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=" + m.group());
                    }
                }
                catch(Exception e)
                {
                    Log.d("XMLParse", e.toString());
                }

                for (String urlString : xmlURLs)
                {
                    try
                    {
                        String download = downloadURL(urlString);
                        l.add(parseXMLString(download));
                        Log.d("XMLParse", "Retrieving GATT information: " + l.size() + "/" + xmlURLs.size());
                    }
                    catch (Exception e)
                    {
                        Log.d("XMLParse", e.toString());
                    }
                }

                listener.onFinish(l);  // Callback to finish build up enum

                return null;
            }
        };

        async.execute();
    }

    enum TagType
    {
        None(null),
        Characteristic("Characteristic"),
        Descriptor("Descriptor"),
        Attribute("Attribute"),
        Service("Service"),
        Name("name"),
        UUID("uuid"),
        Format("Format");

        private String mTagValue;

        TagType(String tagValue)
        {
            mTagValue = tagValue;
        }

        public static TagType map(String tagValue)
        {
            for (TagType tt : TagType.values())
            {
                if (tt.mTagValue == null)
                    continue;

                if (tt.mTagValue.equals(tagValue))
                    return tt;
            }
            return None;
        }
    };

    Map<String, String> parseXMLString(String xmlString) throws XmlPullParserException, IOException
    {
        TagType baseTag = null;  // identifies what kind of document is being parsed
        String name = null;
        String uuid = null;
        String format = null;

        List<TagType> tagStack = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new StringReader(xmlString));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            TagType currentTag = TagType.map(xpp.getName());

            if (eventType == XmlPullParser.START_DOCUMENT)
            {
                // Do nothing
            }
            else if (eventType == XmlPullParser.START_TAG)
            {
                tagStack.add(currentTag);

                if (currentTag == TagType.Characteristic || currentTag == TagType.Descriptor || currentTag == TagType.Attribute || currentTag == TagType.Service)
                {
                    // Parse attribute values
                    name = xpp.getAttributeValue(null, "name");
                    uuid = xpp.getAttributeValue(null, "uuid");
                }

                // TODO: Parse more info depending on tag?

                //System.out.println("Start tag " + xpp.getName());
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                //TODO:  Assert that the last tag in the stack matches our current tag
                tagStack.remove(tagStack.size() - 1);
                // System.out.println("End tag " + xpp.getName());
            }
            else if (eventType == XmlPullParser.TEXT)
            {
                // Inspect top of stack
                currentTag = tagStack.get(tagStack.size() - 1);

                switch (currentTag)
                {
                    case Format:
                        format = xpp.getText();
                }
                // System.out.println("Text " + xpp.getText());
            }
            else
            {
                //System.out.println("Something else " + xpp.getName());
            }
            eventType = xpp.next();
        }

        System.out.println("++-- Extracted info: name=" + name + ", uuid=" + uuid + ", format=" + format);

        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("uuid", uuid);
        m.put("format", format);
        // TODO: Add more attributes to map here?

        return m;
    }

    String downloadURL(String urlString) throws IOException
    {
        URL url = new URL(urlString);
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int read;
        while ((read = in.read(buf)) > 0)
        {
            out.write(buf, 0, read);
        }

        byte raw[] = out.toByteArray();
        String data = new String(raw);

        //Log.d("test", "Recovered data " + data);

        return data;
    }

    private static String getValue(Element element, String tag)
    {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodeList.item(0);
        return node.getNodeValue();
    }
}
