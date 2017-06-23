package com.gattspecparser;

import android.util.Log;
import com.gattspecparser.XMLRipper.onFinishListener;
import com.idevicesinc.sweetblue.utils.Uuids.GATTDisplayType;
import com.idevicesinc.sweetblue.utils.Uuids.GATTFormatType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BuildDescriptorSource
{
    public BuildDescriptorSource()
    {
        new XMLRipper("https://www.bluetooth.com/specifications/gatt/characteristics", Pattern.compile("org\\.bluetooth\\.characteristic\\..*xml"), "GATTDescriptor", new onFinishListener()
        {
            @Override public void onFinish(List<Map<String, String>> l, StringBuilder sb)
            {

                sb.append("\tprivate String mName;\n" +
                        "\tprivate UUID mUUID;\n" +
                        "\tprivate GATTFormatType mFormat;\n" +
                        "\tprivate GATTDisplayType mDisplayType;\n" +
                        "\n" +
                        "\tprivate static Map<UUID, GATTCharacteristic> sUUIDMap = null;\n" +
                        "\n" +
                        "\tGATTCharacteristic(String name, String uuidHex, GATTFormatType format, GATTDisplayType displayType)\n" +
                        "\t{\n" +
                        "\t\tmName = name;\n" +
                        "\t\tmUUID = Uuids.fromShort(uuidHex);\n" +
                        "\t\tmFormat = format;\n" +
                        "\t\tmDisplayType = displayType;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic String getName()\n" +
                        "\t{\n" +
                        "\t\treturn mName;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic UUID getUUID()\n" +
                        "\t{\n" +
                        "\t\treturn mUUID;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic GATTFormatType getFormat()\n" +
                        "\t{\n" +
                        "\t\treturn mFormat;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic GATTDisplayType getDisplayType()\n" +
                        "\t{\n" +
                        "\t\treturn mDisplayType;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic static GATTCharacteristic getCharacteristicForUUID(UUID uuid)\n" +
                        "\t{\n" +
                        "\t\tif (sUUIDMap == null)\n" +
                        "\t\t{\n" +
                        "\t\t\tsUUIDMap = new HashMap<>();\n" +
                        "\n" +
                        "\t\t\tfor (GATTCharacteristic gc : GATTCharacteristic.values())\n" +
                        "\t\t\t\tsUUIDMap.put(gc.getUUID(), gc);\n" +
                        "\t\t}\n" +
                        "\t\treturn sUUIDMap.get(uuid);\n" +
                        "\t}\n" +
                        "}");

                String s = sb.toString();
                System.out.println("enum source:\n" + s);
            }
        });
    }
}
