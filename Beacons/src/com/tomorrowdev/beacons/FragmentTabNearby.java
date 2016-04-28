package com.tomorrowdev.beacons;

import com.viewpagerindicator.CirclePageIndicator;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentTabNearby extends Fragment {

	FlyersViewPager mBeaconScroll;
	BeaconScrollAdapter adapter = null;
	CirclePageIndicator titleIndicator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new BeaconScrollAdapter(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.tab_nearby, container, false);		
		
		//getting the instance of the viewpager by id and setting the adapter
		mBeaconScroll = (FlyersViewPager)view.findViewById(R.id.beaconsScroll);
		adapter.setViewPager(mBeaconScroll);
		mBeaconScroll.setAdapter(adapter);
		
		//getting the instance of the indicator by id
		titleIndicator = (CirclePageIndicator)view.findViewById(R.id.titles);
		titleIndicator.setViewPager(mBeaconScroll);
		titleIndicator.setFillColor(Color.rgb(50, 50, 50));
		
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void addBeacon(BeaconFlyer flyerToAdd){
		//adapter.add(flyerToAdd);
		//el porto a la primera vista de totes, perque en teoria s'afegeixen al principi
		//sino mirar els metodes mostrats a http://stackoverflow.com/questions/13664155/dynamically-add-and-remove-view-to-viewpager
		
		int pageIndex = adapter.add(flyerToAdd, 0);
		mBeaconScroll.setCurrentItem(pageIndex, true);
		mBeaconScroll.setOffscreenPageLimit(adapter.mBeaconsInView.size());
	}
	
	public void removeBeacon(int position){
		adapter.removeView(position);
	}
	
	public void sortByDistance(){
		BeaconFlyer beaconInView = adapter.mBeaconsInView.get(mBeaconScroll.getCurrentItem());
		adapter.sortByDistance();
		mBeaconScroll.setCurrentItem(adapter.getItemPosition(beaconInView));
	}
	
	public void updateBeaconsInView(){
		adapter.filterByCategories();
	}
}
