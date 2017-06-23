package com.gattspecparser;

import android.os.AsyncTask;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Uuids;

import com.idevicesinc.sweetblue.utils.Uuids.GATTDisplayType;
import com.idevicesinc.sweetblue.utils.Uuids.GATTFormatType;
import java.util.Arrays;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class XMLRipper
{
    public interface onFinishListener{
        void onFinish(List<Map<String, String>> l, StringBuilder sb);
    }

    public XMLRipper(final String baseURL, final Pattern baseURLPattern, final String enumName, final onFinishListener listener)
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
                catch(Exception e){
                    Log.d("oops", e.toString());
                }

                for (String urlString : xmlURLs)
                {
                    try
                    {
                        String download = downloadURL(urlString);
                        l.add(parseXMLString(download));
                        Log.d("Progress", "Retrieving GATT information: " + l.size() + "/" + xmlURLs.size());
                    }
                    catch (Exception e)
                    {
                        Log.d("oops", e.toString());
                    }
                }

                StringBuilder sb = startBuildingEnum(l, enumName);  // Start building enum here because it's the same in all cases
                listener.onFinish(l, sb);  // Callback to finish building enum on sb

                return null;
            }
        };

        async.execute();
    }

    private StringBuilder startBuildingEnum(List<Map<String, String>> l, String enumName){
        StringBuilder sb = new StringBuilder();

        sb.append("public enum " + enumName + "\n");
        sb.append("{\n");

        Map<String, GATTDisplayType> formatToGATTDisplayTypeMap = new HashMap<>();
        {
            formatToGATTDisplayTypeMap.put("boolean", GATTDisplayType.Boolean);
            formatToGATTDisplayTypeMap.put("2bit", GATTDisplayType.Bitfield);
            formatToGATTDisplayTypeMap.put("8bit", GATTDisplayType.Bitfield);
            formatToGATTDisplayTypeMap.put("16bit", GATTDisplayType.Bitfield);
            formatToGATTDisplayTypeMap.put("24bit", GATTDisplayType.Bitfield);
            formatToGATTDisplayTypeMap.put("32bit", GATTDisplayType.Bitfield);
            formatToGATTDisplayTypeMap.put("sint8", GATTDisplayType.SignedInteger);
            formatToGATTDisplayTypeMap.put("sint16", GATTDisplayType.SignedInteger);
            formatToGATTDisplayTypeMap.put("sint24", GATTDisplayType.SignedInteger);
            formatToGATTDisplayTypeMap.put("sint32", GATTDisplayType.SignedInteger);
            formatToGATTDisplayTypeMap.put("uint8", GATTDisplayType.UnsignedInteger);
            formatToGATTDisplayTypeMap.put("uint16", GATTDisplayType.UnsignedInteger);
            formatToGATTDisplayTypeMap.put("uint24", GATTDisplayType.UnsignedInteger);
            formatToGATTDisplayTypeMap.put("uint32", GATTDisplayType.UnsignedInteger);
            formatToGATTDisplayTypeMap.put("uint48", GATTDisplayType.UnsignedInteger);
            formatToGATTDisplayTypeMap.put("FLOAT", GATTDisplayType.Decimal);
            formatToGATTDisplayTypeMap.put("SFLOAT", GATTDisplayType.Decimal);
            formatToGATTDisplayTypeMap.put("utf8s", GATTDisplayType.String);
            formatToGATTDisplayTypeMap.put("gatt_uuid", GATTDisplayType.Hex);
            formatToGATTDisplayTypeMap.put("reg-cert-data-list", GATTDisplayType.Hex);
            formatToGATTDisplayTypeMap.put("variable", GATTDisplayType.Hex);
        }

        Map<String, GATTFormatType> formatToGATTFormatTypeMap = new HashMap<>();
        {
            formatToGATTFormatTypeMap.put("boolean", GATTFormatType.GCFT_boolean);
            formatToGATTFormatTypeMap.put("2bit", GATTFormatType.GCFT_2bit);
            formatToGATTFormatTypeMap.put("8bit", GATTFormatType.GCFT_uint8);
            formatToGATTFormatTypeMap.put("16bit", GATTFormatType.GCFT_uint16);
            formatToGATTFormatTypeMap.put("24bit", GATTFormatType.GCFT_uint24);
            formatToGATTFormatTypeMap.put("32bit", GATTFormatType.GCFT_uint32);
            formatToGATTFormatTypeMap.put("sint8", GATTFormatType.GCFT_sint8);
            formatToGATTFormatTypeMap.put("sint16", GATTFormatType.GCFT_sint16);
            formatToGATTFormatTypeMap.put("sint24", GATTFormatType.GCFT_sint24);
            formatToGATTFormatTypeMap.put("sint32", GATTFormatType.GCFT_sint32);
            formatToGATTFormatTypeMap.put("uint8", GATTFormatType.GCFT_uint8);
            formatToGATTFormatTypeMap.put("uint16", GATTFormatType.GCFT_uint16);
            formatToGATTFormatTypeMap.put("uint24", GATTFormatType.GCFT_uint24);
            formatToGATTFormatTypeMap.put("uint32", GATTFormatType.GCFT_uint32);
            formatToGATTFormatTypeMap.put("uint48", GATTFormatType.GCFT_uint48);
            formatToGATTFormatTypeMap.put("FLOAT", GATTFormatType.GCFT_FLOAT);
            formatToGATTFormatTypeMap.put("SFLOAT", GATTFormatType.GCFT_SFLOAT);
            formatToGATTFormatTypeMap.put("utf8s", GATTFormatType.GCFT_utf8s);
            formatToGATTFormatTypeMap.put("gatt_uuid", GATTFormatType.GCFT_struct);
            formatToGATTFormatTypeMap.put("reg-cert-data-list", GATTFormatType.GCFT_struct);
            formatToGATTFormatTypeMap.put("variable", GATTFormatType.GCFT_struct);
        }

        Set<String> otherKeys;  // Other keys to include in enum (all as strings for now)
        otherKeys = l.get(0).keySet();
        otherKeys.removeAll(Arrays.asList("name", "uuid", "format"));

        for (int i = 0; i < l.size(); ++i)
        {
            Map<String, String> m = l.get(i);

            String name = m.get("name");
            String uuid = m.get("uuid");
            String format = m.get("format");
            String nameForEnum = name.replaceAll("\\s", "");
            nameForEnum = nameForEnum.replace("-", "");

            GATTFormatType formatType = formatToGATTFormatTypeMap.get(format);
            if (formatType == null)
                formatType = GATTFormatType.GCFT_struct;

            GATTDisplayType displayType = formatToGATTDisplayTypeMap.get(format);
            if (displayType == null)
                displayType = GATTDisplayType.Hex;

            System.out.println(otherKeys.toString());

            sb.append("\t" + nameForEnum + "(\"" + name + "\", \"" + uuid + "\", GATTFormatType." + formatType.name() + ", GATTDisplayType." + displayType.name());
            for (String key: otherKeys){
                sb.append(", \"" + m.get(key) + "\"");
            }
            sb.append(")");

            if (i < l.size() - 1)
                sb.append(",\n");
            else
                sb.append(";\n\n");
        }
        return sb;
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
