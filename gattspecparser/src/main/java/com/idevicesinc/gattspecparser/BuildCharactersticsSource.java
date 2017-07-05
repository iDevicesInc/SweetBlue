package com.idevicesinc.gattspecparser;

import com.idevicesinc.gattspecparser.XMLRipper.onFinishListener;
import com.idevicesinc.sweetblue.utils.Uuids.GATTDisplayType;
import com.idevicesinc.sweetblue.utils.Uuids.GATTFormatType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

                    sb.append("\t" + nameForEnum + "(\"" + name + "\", \"" + uuid + "\", GATTFormatType." + formatType.name() + ", GATTDisplayType." + displayType.name() + ")");

                    if (i < l.size() - 1)
                        sb.append(",\n");
                    else
                        sb.append(";\n\n");
                }

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
