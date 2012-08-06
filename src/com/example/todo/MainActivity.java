package com.example.todo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.todo.NoteProviderMetaData.NoteTable;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "TODO.MainActivity";

	// UI elements
	private ListView mNotesListView;
	private Button mNewNoteButton;
	private Button mRemoveNotesButton;

	// Components to work with list
	private NoteArrayAdapter mAdapter;
	private boolean[] mSavedSelectedNotes;

	// Service interconnection
	INoteService mService;
	NoteServiceConnection mConnection;

	class NoteServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = INoteService.Stub.asInterface((IBinder) service);
			Log.d(TAG, "onServiceConnected() connected");
			Toast.makeText(MainActivity.this, "Service connected",
					Toast.LENGTH_LONG).show();
			updateNotesList();
		}

		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Log.d(TAG, "onServiceDisconnected() disconnected");
			Toast.makeText(MainActivity.this, "Service disconnected",
					Toast.LENGTH_LONG).show();
		}
	}

	private void initService() {
		mConnection = new NoteServiceConnection();
		Intent intent = new Intent();
		intent.setClassName("com.example.todo",
				com.example.todo.NoteService.class.getName());
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	private void releaseService() {
		unbindService(mConnection);
		mConnection = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initService();

		// Init UI elements
		mNewNoteButton = (Button) findViewById(R.id.newNoteButton);
		mRemoveNotesButton = (Button) findViewById(R.id.removeNotesButton);
		mNewNoteButton.setOnClickListener(this);
		mRemoveNotesButton.setOnClickListener(this);

		mNotesListView = (ListView) findViewById(R.id.notesListView);
	}

	@Override
	protected void onDestroy() {
		releaseService();
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateNotesList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		boolean[] selectedNotes = mAdapter.getSelectedNotes();
		outState.putBooleanArray(TAG, selectedNotes);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// It will be optimized
		if (savedInstanceState.containsKey(TAG)) {
			mSavedSelectedNotes = savedInstanceState.getBooleanArray(TAG);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.testNoteProvider:
			Intent intent = new Intent(MainActivity.this,
					TesterNoteProviderActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newNoteButton:
			Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
			startActivity(intent);
			break;

		case R.id.removeNotesButton:
			break;

		default:
			break;
		}

	}

	private void updateNotesList() {
		if (mService == null) return;
		
		Log.d(TAG, "Update list");
		
		try {
			String notesJson = mService.getNotes();
			
			List<Note> values = new ArrayList<Note>();
			JSONArray notesJsonArray = new JSONArray(notesJson);
			for (int i = 0; i < notesJsonArray.length(); ++i) {
				JSONObject noteJsonObject = notesJsonArray.getJSONObject(i);
				String id = noteJsonObject.getString("id");
				String title = noteJsonObject.getString("title");
				values.add(new Note(id, title));
			}
			mAdapter = new NoteArrayAdapter(this, values);
			mNotesListView.setAdapter(mAdapter);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
