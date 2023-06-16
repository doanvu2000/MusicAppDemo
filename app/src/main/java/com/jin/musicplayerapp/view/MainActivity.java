package com.jin.musicplayerapp.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jin.musicplayerapp.R;
import com.jin.musicplayerapp.adapter.SongsAdapter;
import com.jin.musicplayerapp.databinding.ActivityMainBinding;
import com.jin.musicplayerapp.model.Song;
import com.jin.musicplayerapp.viewmodel.SharedViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //initialize variable
    ActivityResultLauncher<String> storagePermissionLauncher;
    SongsAdapter songsAdapter;
    int gridSpanSize = 1;
    ExoPlayer player;

    SharedViewModel sharedViewModel;
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.playingSongNameView.setSelected(true);//for marquee
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        sharedViewModel.getPlayer().setValue(player);

        //set tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Music Player App");

        //assigning storage permission launcher
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                //permission was granted
                fetchSongs();
            } else {//responding on user's actions
                respondOnUserPermissionActs();
            }
        });

        //launch the storage permission launcher
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);


        //player controls
        playerControls();
    }

    private void playerControls() {
        binding.controlsWrapper.setOnClickListener(view -> {
            PlayerViewFragment playerViewFragment = new PlayerViewFragment();
            String fragmentTag = playerViewFragment.getClass().getName();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment_container, playerViewFragment)
                    .addToBackStack(fragmentTag)
                    .commit();
        });
        binding.nextBtn.setOnClickListener(view -> {
            if (player.hasNextMediaItem()) {
                player.seekToNext();
            }
        });

        binding.prevBtn.setOnClickListener(view -> {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious();
            }
        });

        binding.playPauseBtn.setOnClickListener(view -> {
            if (player.isPlaying()) {
                player.pause();
                binding.playPauseBtn.setImageResource(R.drawable.ic_play_circle);
            } else {
                if (player.getMediaItemCount() > 0) {
                    player.play();
                    binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                }
            }
        });

        //player listener
        playerListener();
    }

    private void playerListener() {
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                assert mediaItem != null;
                binding.playingSongNameView.setText(mediaItem.mediaMetadata.title);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == ExoPlayer.STATE_READY) {
                    binding.playingSongNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();
    }

    //menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.listview) {
            if (gridSpanSize == 1) {
                gridSpanSize = 2;
            } else {
                gridSpanSize = 1;
            }
        }

        GridLayoutManager layoutManager = (GridLayoutManager) binding.recyclerview.getLayoutManager();
        assert layoutManager != null;
        layoutManager.setSpanCount(gridSpanSize);
        //recyclerview.setLayoutManager(layoutManager); //no need
        songsAdapter.notifyDataSetChanged();

        return true;
    }

    private void respondOnUserPermissionActs() {
        //user response
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //permission granted
            fetchSongs();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //show an educational UI to user explaining why we need this permission
            //alert dialog
            new AlertDialog.Builder(this)
                    .setTitle("Requesting Permission")
                    .setMessage("Allow us to fetch & show songs on your device")
                    .setPositiveButton("Allow ", (dialogInterface, i) -> {
                        //request permission again
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    })
                    .setNegativeButton("Don't Allow", (dialogInterface, i) -> {
                        Toast.makeText(getApplicationContext(), " You denied to fetch songs", Toast.LENGTH_LONG).show();
                        dialogInterface.dismiss();
                    })
                    .show();
        } else {
            Toast.makeText(this, "You denied to fetch songs", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchSongs() {
        //define list to carry the songs
        List<Song> songs = new ArrayList<>();
        Uri songLibraryUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            songLibraryUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            songLibraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        //projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        //sort order
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        //Querying
        try (Cursor cursor = getContentResolver().query(songLibraryUri, projection, null, null, sortOrder)) {

            //cache the cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            //getting the values
            while (cursor.moveToNext()) {
                //get values of columns for a give audio file
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                //song uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                //album art uri
                Uri albumartUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                //remove .mp3 extension on song's name
                int lastIndex = name.lastIndexOf(".");
                if (lastIndex > 0) {
                    name = name.substring(0, name.lastIndexOf("."));

                    //song item
                    Song song = new Song(id, uri, name, duration, size, albumId, albumartUri);
                    //add song to songs list
                    songs.add(song);
                }
            }
            //show songs on rv
            showSongs(songs);
            //Toast.makeText(getApplicationContext(), "Number Songs: "+songs.size(),Toast.LENGTH_SHORT).show();
        }
    }

    private void showSongs(List<Song> songs) {
        //layout manager
        //LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, gridSpanSize);
        binding.recyclerview.setLayoutManager(layoutManager);

        //adapter
        songsAdapter = new SongsAdapter(songs, player);
        binding.recyclerview.setAdapter(songsAdapter);
    }
}