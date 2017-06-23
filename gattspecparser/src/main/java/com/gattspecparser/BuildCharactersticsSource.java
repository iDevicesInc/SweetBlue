package com.gattspecparser;

import android.util.Log;
import com.gattspecparser.XMLRipper.onFinishListener;
import com.idevicesinc.sweetblue.utils.Uuids.GATTCharacteristicDisplayType;
import com.idevicesinc.sweetblue.utils.Uuids.GATTCharacteristicFormatType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BuildCharactersticsSource
{
    public BuildCharactersticsSource()
    {
        new XMLRipper("https://www.bluetooth.com/specifications/gatt/characteristics", Pattern.compile("org\\.bluetooth\\.characteristic\\..*xml"), new onFinishListener()
        {
            @Override public void onFinish(List<Map<String, String>> l)
            {
                StringBuilder sb = new StringBuilder();

                sb.append("public enum GATTCharacteristic\n");
                sb.append("{\n");

                Set<String> formatSet = new HashSet<>();

                Map<String, GATTCharacteristicDisplayType> formatToDisplayTypeMap = new HashMap<>();
                {
                    formatToDisplayTypeMap.put("boolean", GATTCharacteristicDisplayType.Boolean);
                    formatToDisplayTypeMap.put("2bit", GATTCharacteristicDisplayType.Bitfield);
                    formatToDisplayTypeMap.put("8bit", GATTCharacteristicDisplayType.Bitfield);
                    formatToDisplayTypeMap.put("16bit", GATTCharacteristicDisplayType.Bitfield);
                    formatToDisplayTypeMap.put("24bit", GATTCharacteristicDisplayType.Bitfield);
                    formatToDisplayTypeMap.put("32bit", GATTCharacteristicDisplayType.Bitfield);
                    formatToDisplayTypeMap.put("sint8", GATTCharacteristicDisplayType.SignedInteger);
                    formatToDisplayTypeMap.put("sint16", GATTCharacteristicDisplayType.SignedInteger);
                    formatToDisplayTypeMap.put("sint24", GATTCharacteristicDisplayType.SignedInteger);
                    formatToDisplayTypeMap.put("sint32", GATTCharacteristicDisplayType.SignedInteger);
                    formatToDisplayTypeMap.put("uint8", GATTCharacteristicDisplayType.UnsignedInteger);
                    formatToDisplayTypeMap.put("uint16", GATTCharacteristicDisplayType.UnsignedInteger);
                    formatToDisplayTypeMap.put("uint24", GATTCharacteristicDisplayType.UnsignedInteger);
                    formatToDisplayTypeMap.put("uint32", GATTCharacteristicDisplayType.UnsignedInteger);
                    formatToDisplayTypeMap.put("uint48", GATTCharacteristicDisplayType.UnsignedInteger);
                    formatToDisplayTypeMap.put("FLOAT", GATTCharacteristicDisplayType.Decimal);
                    formatToDisplayTypeMap.put("SFLOAT", GATTCharacteristicDisplayType.Decimal);
                    formatToDisplayTypeMap.put("utf8s", GATTCharacteristicDisplayType.String);
                    formatToDisplayTypeMap.put("gatt_uuid", GATTCharacteristicDisplayType.Hex);
                    formatToDisplayTypeMap.put("reg-cert-data-list", GATTCharacteristicDisplayType.Hex);
                    formatToDisplayTypeMap.put("variable", GATTCharacteristicDisplayType.Hex);
                }

                Map<String, GATTCharacteristicFormatType> formatToGATTCharacteristicFormatTypeMap = new HashMap<>();
                {
                    formatToGATTCharacteristicFormatTypeMap.put("boolean", GATTCharacteristicFormatType.GCFT_boolean);
                    formatToGATTCharacteristicFormatTypeMap.put("2bit", GATTCharacteristicFormatType.GCFT_2bit);
                    formatToGATTCharacteristicFormatTypeMap.put("8bit", GATTCharacteristicFormatType.GCFT_uint8);
                    formatToGATTCharacteristicFormatTypeMap.put("16bit", GATTCharacteristicFormatType.GCFT_uint16);
                    formatToGATTCharacteristicFormatTypeMap.put("24bit", GATTCharacteristicFormatType.GCFT_uint24);
                    formatToGATTCharacteristicFormatTypeMap.put("32bit", GATTCharacteristicFormatType.GCFT_uint32);
                    formatToGATTCharacteristicFormatTypeMap.put("sint8", GATTCharacteristicFormatType.GCFT_sint8);
                    formatToGATTCharacteristicFormatTypeMap.put("sint16", GATTCharacteristicFormatType.GCFT_sint16);
                    formatToGATTCharacteristicFormatTypeMap.put("sint24", GATTCharacteristicFormatType.GCFT_sint24);
                    formatToGATTCharacteristicFormatTypeMap.put("sint32", GATTCharacteristicFormatType.GCFT_sint32);
                    formatToGATTCharacteristicFormatTypeMap.put("uint8", GATTCharacteristicFormatType.GCFT_uint8);
                    formatToGATTCharacteristicFormatTypeMap.put("uint16", GATTCharacteristicFormatType.GCFT_uint16);
                    formatToGATTCharacteristicFormatTypeMap.put("uint24", GATTCharacteristicFormatType.GCFT_uint24);
                    formatToGATTCharacteristicFormatTypeMap.put("uint32", GATTCharacteristicFormatType.GCFT_uint32);
                    formatToGATTCharacteristicFormatTypeMap.put("uint48", GATTCharacteristicFormatType.GCFT_uint48);
                    formatToGATTCharacteristicFormatTypeMap.put("FLOAT", GATTCharacteristicFormatType.GCFT_FLOAT);
                    formatToGATTCharacteristicFormatTypeMap.put("SFLOAT", GATTCharacteristicFormatType.GCFT_SFLOAT);
                    formatToGATTCharacteristicFormatTypeMap.put("utf8s", GATTCharacteristicFormatType.GCFT_utf8s);
                    formatToGATTCharacteristicFormatTypeMap.put("gatt_uuid", GATTCharacteristicFormatType.GCFT_struct);
                    formatToGATTCharacteristicFormatTypeMap.put("reg-cert-data-list", GATTCharacteristicFormatType.GCFT_struct);
                    formatToGATTCharacteristicFormatTypeMap.put("variable", GATTCharacteristicFormatType.GCFT_struct);
                }

                for (int i = 0; i < l.size(); ++i)
                {
                    Map<String, String> m = l.get(i);

                    String name = m.get("name");
                    String uuid = m.get("uuid");
                    String format = m.get("format");
                    String enumName = name.replaceAll("\\s", "");
                    enumName = enumName.replace("-", "");

                    GATTCharacteristicFormatType formatType = formatToGATTCharacteristicFormatTypeMap.get(format);
                    if (formatType == null)
                        formatType = GATTCharacteristicFormatType.GCFT_struct;

                    GATTCharacteristicDisplayType displayType = formatToDisplayTypeMap.get(format);
                    if (displayType == null)
                        displayType = GATTCharacteristicDisplayType.Hex;

                    String row = "\t" + enumName + "(\"" + name + "\", \"" + uuid + "\", GATTCharacteristicFormatType." + formatType.name() + ", GATTCharacteristicDisplayType." + displayType.name() + ")";

                    sb.append(row);

                    if (i < l.size() - 1)
                        sb.append(",\n");
                    else
                        sb.append(";\n\n");

                    formatSet.add(format);
                }
                sb.append("\tprivate String mName;\n" +
                        "\tprivate UUID mUUID;\n" +
                        "\tprivate GATTCharacteristicFormatType mFormat;\n" +
                        "\tprivate GATTCharacteristicDisplayType mDisplayType;\n" +
                        "\n" +
                        "\tprivate static Map<UUID, GATTCharacteristic> sUUIDMap = null;\n" +
                        "\n" +
                        "\tGATTCharacteristic(String name, String uuidHex, GATTCharacteristicFormatType format, GATTCharacteristicDisplayType displayType)\n" +
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
                        "\tpublic GATTCharacteristicFormatType getFormat()\n" +
                        "\t{\n" +
                        "\t\treturn mFormat;\n" +
                        "\t}\n" +
                        "\n" +
                        "\tpublic GATTCharacteristicDisplayType getDisplayType()\n" +
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

                Log.d("++--", "Discovered formats:");
                for (String f : formatSet)
                {
                    Log.d("++--", "\t" + f);
                }
            }
        });
    }
}
