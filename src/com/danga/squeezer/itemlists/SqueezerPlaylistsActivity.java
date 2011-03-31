package com.danga.squeezer.itemlists;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerActivity;
import com.danga.squeezer.SqueezerBaseListActivity;
import com.danga.squeezer.SqueezerItemView;
import com.danga.squeezer.model.SqueezerPlaylist;

public class SqueezerPlaylistsActivity extends SqueezerBaseListActivity<SqueezerPlaylist>{
	protected static final int DIALOG_NEW = 0;
	protected static final int DIALOG_RENAME = 1;
	protected static final int DIALOG_DELETE = 2;

	private SqueezerPlaylist currentPlaylist;
	private String oldname;

	public SqueezerItemView<SqueezerPlaylist> createItemView() {
		return new SqueezerPlaylistView(this);
	}

	public void registerCallback() throws RemoteException {
		getService().registerPlaylistsCallback(playlistsCallback);
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	public void unregisterCallback() throws RemoteException {
		getService().unregisterPlaylistsCallback(playlistsCallback);
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	public void orderItems(int start) throws RemoteException {
		getService().playlists(start);
	}

	public void onItemSelected(int index, SqueezerPlaylist item) throws RemoteException {
		play(item);
		SqueezerActivity.show(this);
	}
	
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistsmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlists_new:
			showDialog(DIALOG_NEW);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_NEW:
		{
			View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
			builder.setView(form);
	        final EditText editText = (EditText) form.findViewById(R.id.edittext);
			builder.setTitle(R.string.save_playlist_title);
			editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			editText.setHint(R.string.save_playlist_hint);
	        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					create(editText.getText().toString());
				}
			});
	        editText.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	               		create(editText.getText().toString());
						dismissDialog(DIALOG_NEW);
						return true;
	                }
	                return false;
	            }
	        });
        	break;
		}
        case DIALOG_DELETE:
        	{
				currentPlaylist = (SqueezerPlaylist) args.get("playlist");
				builder.setTitle(getString(R.string.delete_title, currentPlaylist.getName()));
				builder.setMessage(R.string.delete__message);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						try {
							getService().playlistsDelete(currentPlaylist);
							orderItems();
						} catch (RemoteException e) {
							Log.e(getTag(), "Error deleting playlist");
						}
					}
				});
			}
			break;
		case DIALOG_RENAME:
			{
		        currentPlaylist = (SqueezerPlaylist) args.get("playlist");
				builder.setTitle(getString(R.string.rename_title, currentPlaylist.getName()));
				View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
				builder.setView(form);
		        final EditText editText = (EditText) form.findViewById(R.id.edittext);
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						rename(currentPlaylist, editText.getText().toString());
					}
				});
			}
			break;
        }
        
        builder.setNegativeButton(android.R.string.cancel, null);
        
        return builder.create();
    }
    
    @Override
    protected void onPrepareDialog(int id, final Dialog dialog, Bundle args) {
        switch (id) {
        case DIALOG_NEW:
        	{
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
				editText.setText("");
        	}
        	break;
        case DIALOG_DELETE:
	        currentPlaylist = (SqueezerPlaylist) args.get("playlist");
        	break;
		case DIALOG_RENAME:
			{
		        currentPlaylist = (SqueezerPlaylist) args.get("playlist");
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
				editText.setText(currentPlaylist.getName());
		        editText.setOnKeyListener(new OnKeyListener() {
		            public boolean onKey(View v, int keyCode, KeyEvent event) {
		                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
							rename(currentPlaylist, editText.getText().toString());
							dialog.dismiss();
							return true;
		                }
		                return false;
		            }
		        });
			}
			break;
        }
    	super.onPrepareDialog(id, dialog, args);
    }
    
    private void create(String name) {
   		try {
			getService().playlistsNew(name);
			orderItems();
		} catch (RemoteException e) {
            Log.e(getTag(), "Error saving playlist as '"+ name + "': " + e);
		}
    }
    
    private void rename(SqueezerPlaylist playlist, String newname) {
   		try {
   			currentPlaylist = playlist;
   			oldname = playlist.getName();
			getService().playlistsRename(playlist, newname);
			playlist.setName(newname);
			getItemListAdapter().notifyDataSetChanged();
		} catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
		}
    }

    
	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlaylistsActivity.class);
        context.startActivity(intent);
    }

    private IServicePlaylistsCallback playlistsCallback = new IServicePlaylistsCallback.Stub() {
		public void onPlaylistsReceived(int count, int max, int start, List<SqueezerPlaylist> items) throws RemoteException {
			onItemsReceived(count, max, start, items);
		}
    };
    
    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				getItemListAdapter().notifyDataSetChanged();
				Toast.makeText(SqueezerPlaylistsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    private IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		public void onRenameFailed(String msg) throws RemoteException {
			currentPlaylist.setName(oldname);
			showServiceMessage(msg);
		}

		public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}
    	
    };

}
