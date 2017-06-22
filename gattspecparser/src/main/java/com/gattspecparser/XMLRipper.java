package com.gattspecparser;

import android.os.AsyncTask;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Uuids;

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
    private static String xmlURLs[] =
    {
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.aerobic_heart_rate_lower_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.aerobic_heart_rate_upper_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.aerobic_threshold.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.age.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.aggregate.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.alert_category_id.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.alert_category_id_bit_mask.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.alert_level.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.alert_notification_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.alert_status.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.altitude.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.anaerobic_heart_rate_lower_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.anaerobic_heart_rate_upper_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.anaerobic_threshold.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.analog.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.apparent_wind_direction.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.apparent_wind_speed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.appearance.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.barometric_pressure_trend.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.battery_level.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.blood_pressure_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.blood_pressure_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.body_composition_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.body_composition_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.body_sensor_location.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.bond_management_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.bond_management_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.boot_keyboard_input_report.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.boot_keyboard_output_report.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.boot_mouse_input_report.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.central_address_resolution_support.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_session_run_time.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_session_start_time.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_specific_ops_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cgm_status.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cross_trainer_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.csc_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.csc_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.current_time.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cycling_power_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cycling_power_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cycling_power_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.cycling_power_vector.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.database_change_increment.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.date_of_birth.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.date_of_threshold_assessment.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.date_time.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.day_date_time.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.day_of_week.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.descriptor_value_changed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.device_name.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.dew_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.digital.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.dst_offset.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.elevation.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.email_address.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.exact_time_256.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.fat_burn_heart_rate_lower_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.fat_burn_heart_rate_upper_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.firmware_revision_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.first_name.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.fitness_machine_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.fitness_machine_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.fitness_machine_status.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.five_zone_heart_rate_limits.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.floor_number.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gender.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.glucose_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.glucose_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.glucose_measurement_context.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gust_factor.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.hardware_revision_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.heart_rate_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.heart_rate_max.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.heart_rate_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.heat_index.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.height.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.hid_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.hid_information.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.hip_circumference.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.http_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.http_entity_body.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.http_headers.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.http_status_code.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.https_security.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.humidity.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.indoor_bike_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.indoor_positioning_configuration.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.intermediate_cuff_pressure.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.intermediate_temperature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.irradiance.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.language.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.last_name.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.latitude.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ln_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ln_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.local_east_coordinate.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.local_north_coordinate.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.local_time_information.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.location_and_speed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.location_name.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.longitude.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.magnetic_declination.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.Magnetic_flux_density_2D.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.Magnetic_flux_density_3D.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.manufacturer_name_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.maximum_recommended_heart_rate.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.measurement_interval.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.model_number_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.navigation.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.new_alert.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_action_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_changed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_first_created.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_id.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_last_modified.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_list_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_list_filter.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_name.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_properties.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_size.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.object_type.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ots_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.peripheral_privacy_flag.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.plx_continuous_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.plx_features.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.plx_spot_check_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.pnp_id.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.pollen_concentration.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.position_quality.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.pressure.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.protocol_mode.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.rainfall.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gap.reconnection_address.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.record_access_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.reference_time_information.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.report.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.report_map.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.resolvable_private_address_only.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.resting_heart_rate.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ringer_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.ringer_setting.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.rower_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.rsc_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.rsc_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.sc_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.scan_interval_window.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.scan_refresh.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.sensor_location.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.serial_number_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.gatt.service_changed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.software_revision_string.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.sport_type_for_aerobic_and_anaerobic_thresholds.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.stair_climber_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.step_climber_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_heart_rate_range.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_inclination_range.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_new_alert_category.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_power_range.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_resistance_level_range.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_speed_range.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.supported_unread_alert_category.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.system_id.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.tds_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.temperature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.temperature_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.temperature_type.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.three_zone_heart_rate_limits.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_accuracy.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_source.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_update_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_update_state.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_with_dst.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.time_zone.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.training_status.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.treadmill_data.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.true_wind_direction.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.true_wind_speed.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.two_zone_heart_rate_limit.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.tx_power_level.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.uncertainty.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.unread_alert_status.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.uri.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.user_control_point.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.user_index.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.uv_index.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.vo2_max.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.waist_circumference.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.weight.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.weight_measurement.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.weight_scale_feature.xml",
        "https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.wind_chill.xml"
    };


    public XMLRipper()
    {
        AsyncTask<String[], Void, Void> async = new AsyncTask<String[], Void, Void>()
        {
            @Override
            protected Void doInBackground(String[]... params)
            {
                System.out.println("++-- Starting async task");

                List<Map<String, String>> l = new ArrayList<>();

                //FIXME:  Figure out how to pass in array
                for (String urlString : xmlURLs)
                {
                    try
                    {
                        String download = downloadURL(urlString);
                        l.add(parseXMLString(download));
                    }
                    catch (Exception e)
                    {
                        Log.d("oops", e.toString());
                    }
                }

                generateEnumSourceCode(l);

                return null;
            }
        };

        async.execute(xmlURLs);
    }

    enum TagType
    {
        None(null),
        Characteristic("Characteristic"),
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
                System.out.println("Start document");
            }
            else if (eventType == XmlPullParser.START_TAG)
            {
                tagStack.add(currentTag);

                if (currentTag == TagType.Characteristic)
                {
                    // Parse attribute values
                    name = xpp.getAttributeValue(null, "name");
                    uuid = xpp.getAttributeValue(null, "uuid");
                }

                System.out.println("Start tag " + xpp.getName());
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                //TODO:  Assert that the last tag in the stack matches our current tag
                tagStack.remove(tagStack.size() - 1);
                System.out.println("End tag " + xpp.getName());
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

                System.out.println("Text " + xpp.getText());
            }
            else
            {
                System.out.println("Something else " + xpp.getName());
            }
            eventType = xpp.next();
        }

        System.out.println("End document");


        System.out.println("++-- Extracted info: name=" + name + ", uuid=" + uuid + ", format=" + format);

        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("uuid", uuid);
        m.put("format", format);

        return m;
    }

    enum GATTCharacteristicFormatType
    {
        GCFT_rfu("rfu", "Reserved for future use", false),
        GCFT_boolean("boolean", "unsigned 1-bit; 0=false, 1=true", false),
        GCFT_2bit("2bit", "unsigned 2-bit integer", false),
        GCFT_nibble("nibble", "unsigned 4-bit integer", false),
        GCFT_uint8("uint8", "unsigned 8-bit integer", true),
        GCFT_uint12("uint12", "unsigned 12-bit integer", true),
        GCFT_uint16("uint16", "unsigned 16-bit integer", true),
        GCFT_uint24("uint24", "unsigned 24-bit integer", true),
        GCFT_uint32("uint32", "unsigned 32-bit integer", true),
        GCFT_uint48("uint48", "unsigned 48-bit integer", true),
        GCFT_uint64("uint64", "unsigned 64-bit integer", true),
        GCFT_uint128("uint128", "unsigned 128-bit integer", true),
        GCFT_sint8("sint8", "signed 8-bit integer", true),
        GCFT_sint12("sint12", "signed 12-bit integer", true),
        GCFT_sint16("sint16", "signed 16-bit integer", true),
        GCFT_sint24("sint24", "signed 24-bit integer", true),
        GCFT_sint32("sint32", "signed 32-bit integer", true),
        GCFT_sint48("sint48", "signed 48-bit integer", true),
        GCFT_sint64("sint64", "signed 64-bit integer", true),
        GCFT_sint128("sint128", "signed 128-bit integer", true),
        GCFT_float32("float32", "IEEE-754 32-bit floating point", false),
        GCFT_float64("float64", "IEEE-754 64-bit floating point", false),
        GCFT_SFLOAT("SFLOAT", "IEEE-11073 16-bit SFLOAT", false),
        GCFT_FLOAT("FLOAT", "IEEE-11073 32-bit FLOAT", false),
        GCFT_duint16("duint16", "IEEE-20601 format", false),
        GCFT_utf8s("utf8s", "UTF-8 string", false),
        GCFT_utf16s("utf16s", "UTF-16 string", false),
        GCFT_struct("struct", "Opaque structure", false)
        // Remaining values RFU
        ;

        private String mShortName;
        private String mDescription;
        private boolean mExponentValue;

        GATTCharacteristicFormatType(String shortName, String description, boolean exponentValue)
        {
            mShortName = shortName;
            mDescription = description;
            mExponentValue = exponentValue;
        }

        public String getShortName()
        {
            return mShortName;
        }

        public String getDescription()
        {
            return mDescription;
        }

        public boolean getExponentValue()
        {
            return mExponentValue;
        }
    }

    enum GATTCharacteristicDisplayType
    {
        Boolean,
        Bitfield,
        UnsignedInteger,
        SignedInteger,
        Decimal,
        String,
        Hex
    };

    public enum GATTCharacteristic
    {
        AerobicHeartRateLowerLimit("Aerobic Heart Rate Lower Limit", "2A7E", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AerobicHeartRateUpperLimit("Aerobic Heart Rate Upper Limit", "2A84", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AerobicThreshold("Aerobic Threshold", "2A7F", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Age("Age", "2A80", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Aggregate("Aggregate", "2A5A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        AlertCategoryID("Alert Category ID", "2A43", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AlertCategoryIDBitMask("Alert Category ID Bit Mask", "2A42", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AlertLevel("Alert Level", "2A06", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AlertNotificationControlPoint("Alert Notification Control Point", "2A44", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AlertStatus("Alert Status", "2A3F", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Altitude("Altitude", "2AB3", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        AnaerobicHeartRateLowerLimit("Anaerobic Heart Rate Lower Limit", "2A81", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AnaerobicHeartRateUpperLimit("Anaerobic Heart Rate Upper Limit", "2A82", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        AnaerobicThreshold("Anaerobic Threshold", "2A83", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Analog("Analog", "2A58", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        ApparentWindDirection("Apparent Wind Direction", "2A73", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        ApparentWindSpeed("Apparent Wind Speed", "2A72", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        Appearance("Appearance", "2A01", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        BarometricPressureTrend("Barometric Pressure Trend", "2AA3", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        BatteryLevel("Battery Level", "2A19", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        BloodPressureFeature("Blood Pressure Feature", "2A49", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        BloodPressureMeasurement("Blood Pressure Measurement", "2A35", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        BodyCompositionFeature("Body Composition Feature", "2A9B", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
        BodyCompositionMeasurement("Body Composition Measurement", "2A9C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        BodySensorLocation("Body Sensor Location", "2A38", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        BondManagementControlPoint("Bond Management Control Point", "2AA4", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        BondManagementFeatures("Bond Management Features", "2AA5", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.Bitfield),
        BootKeyboardInputReport("Boot Keyboard Input Report", "2A22", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        BootKeyboardOutputReport("Boot Keyboard Output Report", "2A32", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        BootMouseInputReport("Boot Mouse Input Report", "2A33", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        CentralAddressResolution("Central Address Resolution", "2AA6", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        CGMFeature("CGM Feature", "2AA8", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CGMMeasurement("CGM Measurement", "2AA7", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CGMSessionRunTime("CGM Session Run Time", "2AAB", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CGMSessionStartTime("CGM Session Start Time", "2AAA", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CGMSpecificOpsControlPoint("CGM Specific Ops Control Point", "2AAC", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        CGMStatus("CGM Status", "2AA9", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CrossTrainerData("Cross Trainer Data", "2ACE", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CSCFeature("CSC Feature", "2A5C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        CSCMeasurement("CSC Measurement", "2A5B", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CurrentTime("Current Time", "2A2B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        CyclingPowerControlPoint("Cycling Power Control Point", "2A66", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        CyclingPowerFeature("Cycling Power Feature", "2A65", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
        CyclingPowerMeasurement("Cycling Power Measurement", "2A63", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        CyclingPowerVector("Cycling Power Vector", "2A64", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        DatabaseChangeIncrement("Database Change Increment", "2A99", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
        DateofBirth("Date of Birth", "2A85", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        DateofThresholdAssessment("Date of Threshold Assessment", "2A86", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        DateTime("Date Time", "2A08", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        DayDateTime("Day Date Time", "2A0A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        DayofWeek("Day of Week", "2A09", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        DescriptorValueChanged("Descriptor Value Changed", "2A7D", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        DeviceName("Device Name", "2A00", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        DewPoint("Dew Point", "2A7B", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
        Digital("Digital", "2A56", GATTCharacteristicFormatType.GCFT_2bit, GATTCharacteristicDisplayType.Bitfield),
        DSTOffset("DST Offset", "2A0D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Elevation("Elevation", "2A6C", GATTCharacteristicFormatType.GCFT_sint24, GATTCharacteristicDisplayType.SignedInteger),
        EmailAddress("Email Address", "2A87", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        ExactTime256("Exact Time 256", "2A0C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        FatBurnHeartRateLowerLimit("Fat Burn Heart Rate Lower Limit", "2A88", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        FatBurnHeartRateUpperLimit("Fat Burn Heart Rate Upper Limit", "2A89", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        FirmwareRevisionString("Firmware Revision String", "2A26", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        FirstName("First Name", "2A8A", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        FitnessMachineControlPoint("Fitness Machine Control Point", "2AD9", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        FitnessMachineFeature("Fitness Machine Feature", "2ACC", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        FitnessMachineStatus("Fitness Machine Status", "2ADA", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        FiveZoneHeartRateLimits("Five Zone Heart Rate Limits", "2A8B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        FloorNumber("Floor Number", "2AB2", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Gender("Gender", "2A8C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        GlucoseFeature("Glucose Feature", "2A51", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        GlucoseMeasurement("Glucose Measurement", "2A18", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        GlucoseMeasurementContext("Glucose Measurement Context", "2A34", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
        GustFactor("Gust Factor", "2A74", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        HardwareRevisionString("Hardware Revision String", "2A27", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        HeartRateControlPoint("Heart Rate Control Point", "2A39", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        HeartRateMax("Heart Rate Max", "2A8D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        HeartRateMeasurement("Heart Rate Measurement", "2A37", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        HeatIndex("Heat Index", "2A7A", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
        Height("Height", "2A8E", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        HIDControlPoint("HID Control Point", "2A4C", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        HIDInformation("HID Information", "2A4A", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        HipCircumference("Hip Circumference", "2A8F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        HTTPControlPoint("HTTP Control Point", "2ABA", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        HTTPEntityBody("HTTP Entity Body", "2AB9", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        HTTPHeaders("HTTP Headers", "2AB7", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        HTTPStatusCode("HTTP Status Code", "2AB8", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        HTTPSSecurity("HTTPS Security", "2ABB", GATTCharacteristicFormatType.GCFT_boolean, GATTCharacteristicDisplayType.Boolean),
        Humidity("Humidity", "2A6F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        IEEE1107320601RegulatoryCertificationDataList("IEEE 11073-20601 Regulatory Certification Data List", "2A2A", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        IndoorBikeData("Indoor Bike Data", "2AD2", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        IndoorPositioningConfiguration("Indoor Positioning Configuration", "2AAD", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        IntermediateCuffPressure("Intermediate Cuff Pressure", "2A36", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        IntermediateTemperature("Intermediate Temperature", "2A1E", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        Irradiance("Irradiance", "2A77", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        Language("Language", "2AA2", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        LastName("Last Name", "2A90", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        Latitude("Latitude", "2AAE", GATTCharacteristicFormatType.GCFT_sint32, GATTCharacteristicDisplayType.SignedInteger),
        LNControlPoint("LN Control Point", "2A6B", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        LNFeature("LN Feature", "2A6A", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
        LocalEastCoordinate("Local East Coordinate", "2AB1", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        LocalNorthCoordinate("Local North Coordinate", "2AB0", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        LocalTimeInformation("Local Time Information", "2A0F", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        LocationandSpeedCharacteristic("Location and Speed Characteristic", "2A67", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        LocationName("Location Name", "2AB5", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        Longitude("Longitude", "2AAF", GATTCharacteristicFormatType.GCFT_sint32, GATTCharacteristicDisplayType.SignedInteger),
        MagneticDeclination("Magnetic Declination", "2A2C", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        MagneticFluxDensity2D("Magnetic Flux Density - 2D", "2AA0", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        MagneticFluxDensity3D("Magnetic Flux Density - 3D", "2AA1", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        ManufacturerNameString("Manufacturer Name String", "2A29", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        MaximumRecommendedHeartRate("Maximum Recommended Heart Rate", "2A91", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        MeasurementInterval("Measurement Interval", "2A21", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        ModelNumberString("Model Number String", "2A24", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        Navigation("Navigation", "2A68", GATTCharacteristicFormatType.GCFT_sint24, GATTCharacteristicDisplayType.SignedInteger),
        NewAlert("New Alert", "2A46", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        ObjectActionControlPoint("Object Action Control Point", "2AC5", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectChanged("Object Changed", "2AC8", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectFirstCreated("Object First-Created", "2AC1", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectID("Object ID", "2AC3", GATTCharacteristicFormatType.GCFT_uint48, GATTCharacteristicDisplayType.UnsignedInteger),
        ObjectLastModified("Object Last-Modified", "2AC2", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectListControlPoint("Object List Control Point", "2AC6", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectListFilter("Object List Filter", "2AC7", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectName("Object Name", "2ABE", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        ObjectProperties("Object Properties", "2AC4", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ObjectSize("Object Size", "2AC0", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
        ObjectType("Object Type", "2ABF", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        OTSFeature("OTS Feature", "2ABD", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        PeripheralPreferredConnectionParameters("Peripheral Preferred Connection Parameters", "2A04", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        PeripheralPrivacyFlag("Peripheral Privacy Flag", "2A02", GATTCharacteristicFormatType.GCFT_boolean, GATTCharacteristicDisplayType.Boolean),
        PLXContinuousMeasurementCharacteristic("PLX Continuous Measurement Characteristic", "2A5F", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
        PLXFeatures("PLX Features", "2A60", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.Bitfield),
        PLXSpotCheckMeasurement("PLX Spot-Check Measurement", "2A5E", GATTCharacteristicFormatType.GCFT_SFLOAT, GATTCharacteristicDisplayType.Decimal),
        PnPID("PnP ID", "2A50", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        PollenConcentration("Pollen Concentration", "2A75", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.UnsignedInteger),
        PositionQuality("Position Quality", "2A69", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Pressure("Pressure", "2A6D", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
        ProtocolMode("Protocol Mode", "2A4E", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Rainfall("Rainfall", "2A78", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        ReconnectionAddress("Reconnection Address", "2A03", GATTCharacteristicFormatType.GCFT_uint48, GATTCharacteristicDisplayType.UnsignedInteger),
        RecordAccessControlPoint("Record Access Control Point", "2A52", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ReferenceTimeInformation("Reference Time Information", "2A14", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        Report("Report", "2A4D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        ReportMap("Report Map", "2A4B", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        ResolvablePrivateAddressOnly("Resolvable Private Address Only", "2AC9", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        RestingHeartRate("Resting Heart Rate", "2A92", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        RingerControlpoint("Ringer Control point", "2A40", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        RingerSetting("Ringer Setting", "2A41", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        RowerData("Rower Data", "2AD1", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        RSCFeature("RSC Feature", "2A54", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.Bitfield),
        RSCMeasurement("RSC Measurement", "2A53", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.UnsignedInteger),
        SCControlPoint("SC Control Point", "2A55", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        ScanIntervalWindow("Scan Interval Window", "2A4F", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        ScanRefresh("Scan Refresh", "2A31", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        SensorLocation("Sensor Location", "2A5D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        SerialNumberString("Serial Number String", "2A25", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        ServiceChanged("Service Changed", "2A05", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SoftwareRevisionString("Software Revision String", "2A28", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        SportTypeforAerobicandAnaerobicThresholds("Sport Type for Aerobic and Anaerobic Thresholds", "2A93", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        StairClimberData("Stair Climber Data", "2AD0", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        StepClimberData("Step Climber Data", "2ACF", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedHeartRateRange("Supported Heart Rate Range", "2AD7", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedInclinationRange("Supported Inclination Range", "2AD5", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedNewAlertCategory("Supported New Alert Category", "2A47", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        SupportedPowerRange("Supported Power Range", "2AD8", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedResistanceLevelRange("Supported Resistance Level Range", "2AD6", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedSpeedRange("Supported Speed Range", "2AD4", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        SupportedUnreadAlertCategory("Supported Unread Alert Category", "2A48", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        SystemID("System ID", "2A23", GATTCharacteristicFormatType.GCFT_uint24, GATTCharacteristicDisplayType.UnsignedInteger),
        TDSControlPoint("TDS Control Point", "2ABC", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        Temperature("Temperature", "2A6E", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        TemperatureMeasurement("Temperature Measurement", "2A1C", GATTCharacteristicFormatType.GCFT_FLOAT, GATTCharacteristicDisplayType.Decimal),
        TemperatureType("Temperature Type", "2A1D", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        ThreeZoneHeartRateLimits("Three Zone Heart Rate Limits", "2A94", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        TimeAccuracy("Time Accuracy", "2A12", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        TimeSource("Time Source", "2A13", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.Bitfield),
        TimeUpdateControlPoint("Time Update Control Point", "2A16", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        TimeUpdateState("Time Update State", "2A17", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        TimewithDST("Time with DST", "2A11", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        TimeZone("Time Zone", "2A0E", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
        TrainingStatus("Training Status", "2AD3", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        TreadmillData("Treadmill Data", "2ACD", GATTCharacteristicFormatType.GCFT_sint16, GATTCharacteristicDisplayType.SignedInteger),
        TrueWindDirection("True Wind Direction", "2A71", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        TrueWindSpeed("True Wind Speed", "2A70", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        TwoZoneHeartRateLimit("Two Zone Heart Rate Limit", "2A95", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        TxPowerLevel("Tx Power Level", "2A07", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger),
        Uncertainty("Uncertainty", "2AB4", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        UnreadAlertStatus("Unread Alert Status", "2A45", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        URI("URI", "2AB6", GATTCharacteristicFormatType.GCFT_utf8s, GATTCharacteristicDisplayType.String),
        UserControlPoint("User Control Point", "2A9F", GATTCharacteristicFormatType.GCFT_struct, GATTCharacteristicDisplayType.Hex),
        UserIndex("User Index", "2A9A", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        UVIndex("UV Index", "2A76", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        VO2Max("VO2 Max", "2A96", GATTCharacteristicFormatType.GCFT_uint8, GATTCharacteristicDisplayType.UnsignedInteger),
        WaistCircumference("Waist Circumference", "2A97", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        Weight("Weight", "2A98", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        WeightMeasurement("Weight Measurement", "2A9D", GATTCharacteristicFormatType.GCFT_uint16, GATTCharacteristicDisplayType.UnsignedInteger),
        WeightScaleFeature("Weight Scale Feature", "2A9E", GATTCharacteristicFormatType.GCFT_uint32, GATTCharacteristicDisplayType.Bitfield),
        WindChill("Wind Chill", "2A79", GATTCharacteristicFormatType.GCFT_sint8, GATTCharacteristicDisplayType.SignedInteger);

        private String mName;
        private UUID mUUID;
        private GATTCharacteristicFormatType mFormat;
        private GATTCharacteristicDisplayType mDisplayType;

        private static Map<UUID, GATTCharacteristic> sUUIDMap = null;

        GATTCharacteristic(String name, String uuidHex, GATTCharacteristicFormatType format, GATTCharacteristicDisplayType displayType)
        {
            mName = name;
            mUUID = Uuids.fromShort(uuidHex);
            mFormat = format;
            mDisplayType = displayType;
        }

        public String getName()
        {
            return mName;
        }

        public UUID getUUID()
        {
            return mUUID;
        }

        public GATTCharacteristicFormatType getFormat()
        {
            return mFormat;
        }

        public GATTCharacteristicDisplayType getDisplayType()
        {
            return mDisplayType;
        }

        public static GATTCharacteristic getCharacteristicForUUID(UUID uuid)
        {
            if (sUUIDMap == null)
            {
                sUUIDMap = new HashMap<>();

                for (GATTCharacteristic gc : GATTCharacteristic.values())
                    sUUIDMap.put(gc.getUUID(), gc);
            }
            return sUUIDMap.get(uuid);
        }
    }

    void generateEnumSourceCode(List<Map<String, String>> l)
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
        Log.d("++--", "enum source:\n" + s);

        Log.d("++--", "Discovered formats:");
        for (String f : formatSet)
        {
            Log.d("++--", "\t" + f);
        }
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

        Log.d("test", "Recovered data " + data);

        return data;
    }

    private static String getValue(Element element, String tag)
    {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodeList.item(0);
        return node.getNodeValue();
    }
}
