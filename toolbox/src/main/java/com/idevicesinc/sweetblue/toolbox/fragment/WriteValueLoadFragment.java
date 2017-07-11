package com.idevicesinc.sweetblue.toolbox.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.WriteValueActivity;

import java.util.List;


public class WriteValueLoadFragment extends Fragment
{
    WriteValueActivity mParent;

    SavedValueAdapter mAdapter;
    ListView mListView;
    List<WriteValueActivity.SavedValue> mSavedValueList = null;
    WriteValueActivity.SavedValue mSelectedValue = null;

    public static WriteValueLoadFragment newInstance(WriteValueActivity parent, List<WriteValueActivity.SavedValue> savedValueList)
    {
        WriteValueLoadFragment wvlf = new WriteValueLoadFragment();
        wvlf.mParent = parent;
        wvlf.mSavedValueList = savedValueList;
        return wvlf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.layout_write_value_load, null);

        mListView = (ListView)layout.findViewById(R.id.savedValueListView);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                view.setBackgroundColor(getResources().getColor(R.color.light_blue));
                mSelectedValue = mAdapter.getItem(position);

                for (int i = 0; i < parent.getChildCount(); ++i)
                {
                    View child = parent.getChildAt(i);
                    if (child == null)
                        continue;

                    int pos = parent.getFirstVisiblePosition() + i;
                    child.setBackgroundColor(getResources().getColor(pos == position ? R.color.very_light_blue : R.color.white));
                }

            }
        });

        if (mSavedValueList != null)
        {
            mAdapter = new SavedValueAdapter(getContext(), mSavedValueList);
            mListView.setAdapter(mAdapter);
        }

        return layout;
    }

    public WriteValueActivity.SavedValue getSelectedValue()
    {
        return mSelectedValue;
    }

    private class SavedValueAdapter extends ArrayAdapter<WriteValueActivity.SavedValue>
    {
        public SavedValueAdapter(Context context, List<WriteValueActivity.SavedValue> savedValues)
        {
            super(context, 0, savedValues);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            final WriteValueActivity.SavedValue sv = getItem(position);
            String names[] = getResources().getStringArray(R.array.gatt_format_type_names);

            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.saved_value_layout, parent, false);

            TextView valueNameLabel = (TextView)convertView.findViewById(R.id.valueNameLabel);
            TextView valueTypeLabel = (TextView)convertView.findViewById(R.id.valueTypeLabel);
            TextView valueLabel = (TextView)convertView.findViewById(R.id.valueLabel);

            valueNameLabel.setText(sv.getName());
            valueTypeLabel.setText(names[sv.getGATTFormatType().ordinal()]);
            valueLabel.setText(sv.getValueString());

            convertView.setBackgroundColor(getResources().getColor(sv.equals(mSelectedValue) ? R.color.very_light_blue : R.color.white));

            // Bind (or re-bind) the fake oveflow menu
            View v = convertView.findViewById(R.id.fakeOverflowMenu);
            final View anchor = convertView.findViewById(R.id.fakeOverflowMenuAnchor);
            v.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //Creating the instance of PopupMenu
                    PopupMenu popup = new PopupMenu(getContext(), anchor);
                    //Inflating the Popup using xml file
                    popup.getMenuInflater().inflate(R.menu.saved_value_popup, popup.getMenu());

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                    {
                        public boolean onMenuItemClick(MenuItem item)
                        {
                            switch (item.getItemId())
                            {
                                case R.id.writeValueEdit:
                                    mParent.editSavedValue(sv);
                                    break;

                                case R.id.writeValueDelete:
                                    mParent.deleteSavedValue(sv);
                                    mSavedValueList.remove(sv);
                                    mAdapter.notifyDataSetChanged();
                                    if (sv.equals(mSelectedValue))
                                        mSelectedValue = null;
                                    break;
                            }
                            return true;
                        }
                    });

                    popup.show();
                }
            });

            return convertView;
        }
    }

}
