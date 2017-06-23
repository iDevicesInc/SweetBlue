package com.gattspecparser;

import android.os.AsyncTask;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Uuids;

import com.idevicesinc.sweetblue.utils.Uuids.GATTCharacteristicDisplayType;
import com.idevicesinc.sweetblue.utils.Uuids.GATTCharacteristicFormatType;
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
                ArrayList<String> xmlURLs = new ArrayList<>();
                try
                {
                    String characteristicsPageHTML = downloadURL("https://www.bluetooth.com/specifications/gatt/characteristics");
                    Pattern p = Pattern.compile("org\\.bluetooth\\.characteristic\\..*xml");
                    Matcher m = p.matcher(characteristicsPageHTML);
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
                        Log.d("Progress", "Retrieving GATT characteristics: " + l.size() + "/" + xmlURLs.size());
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

        async.execute();
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
        {TagType currentTag = TagType.map(xpp.getName());

            if (eventType == XmlPullParser.START_DOCUMENT)
            {
                // Do nothing
                // System.out.println("Start document");
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

        System.out.println("End document");


        System.out.println("++-- Extracted info: name=" + name + ", uuid=" + uuid + ", format=" + format);

        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("uuid", uuid);
        m.put("format", format);

        return m;
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
        System.out.println("enum source:\n" + s);

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
