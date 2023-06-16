package com.jin.musicplayerapp.view;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jin.musicplayerapp.R;
import com.jin.musicplayerapp.databinding.FragmentPlayerViewBinding;
import com.jin.musicplayerapp.viewmodel.SharedViewModel;

import java.util.Objects;

public class PlayerViewFragment extends Fragment {
    FragmentPlayerViewBinding binding;
    SharedViewModel sharedViewModel;
    ExoPlayer player;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPlayerViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //assign
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        //back btn clicked
        binding.backBtn.setOnClickListener(view1 -> requireActivity().onBackPressed());

        //getting the player
        gettingPlayer();

    }

    private void gettingPlayer() {
        sharedViewModel.getPlayer().observe(requireActivity(), livePlayer -> {
            if (livePlayer != null) {
                player = livePlayer;
                //player controls
                playerControls(player);
            }
        });
    }

    private void playerControls(ExoPlayer player) {
        //player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                assert mediaItem != null;
                binding.titleView.setText(mediaItem.mediaMetadata.title);
                binding.progressDuration.setText(getReadableTime((int) player.getCurrentPosition()));
                binding.seekBar.setProgress((int) player.getCurrentPosition());
                binding.totalDuration.setText(getReadableTime((int) player.getDuration()));
                binding.seekBar.setMax((int) player.getDuration());
                binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                showCurrentArtwork();
                updatePlayerPositionProgress();
                binding.artworkView.setAnimation(loadRotation());
                if (!player.isPlaying()) {
                    player.play();
                }

            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == ExoPlayer.STATE_READY) {
                    binding.titleView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                    binding.progressDuration.setText(getReadableTime((int) player.getCurrentPosition()));
                    binding.seekBar.setProgress((int) player.getCurrentPosition());
                    binding.totalDuration.setText(getReadableTime((int) player.getDuration()));
                    binding.seekBar.setMax((int) player.getDuration());
                    showCurrentArtwork();
                    updatePlayerPositionProgress();
                    binding.artworkView.setAnimation(loadRotation());

                } else {
                    binding.playPauseBtn.setImageResource(R.drawable.ic_play_circle);
                }
            }
        });
        //checking if the player is playing
        if (player.isPlaying()) {
            binding.titleView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
            binding.progressDuration.setText(getReadableTime((int) player.getCurrentPosition()));
            binding.seekBar.setProgress((int) player.getCurrentPosition());
            binding.totalDuration.setText(getReadableTime((int) player.getDuration()));
            binding.seekBar.setMax((int) player.getDuration());
            binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);

            showCurrentArtwork();
            updatePlayerPositionProgress();
            binding.artworkView.setAnimation(loadRotation());
        }

        //player btns
        binding.nextBtn.setOnClickListener(view -> {
            if (player.hasNextMediaItem()) {
                player.seekToNext();
                showCurrentArtwork();
                updatePlayerPositionProgress();
                binding.artworkView.setAnimation(loadRotation());
            }
        });

        binding.prevBtn.setOnClickListener(view -> {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious();
                showCurrentArtwork();
                updatePlayerPositionProgress();
                binding.artworkView.setAnimation(loadRotation());
            }
        });

        binding.playPauseBtn.setOnClickListener(view -> {
            if (player.isPlaying()) {
                player.pause();
                binding.playPauseBtn.setImageResource(R.drawable.ic_play_circle);
                binding.artworkView.clearAnimation();
            } else {
                player.play();
                binding.playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                binding.artworkView.setAnimation(loadRotation());
            }
        });

        //set seek bar change listener
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(progressValue);
                binding.progressDuration.setText(getReadableTime(progressValue));
                player.seekTo(progressValue);
            }
        });
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(() -> {
            if (player.isPlaying()) {
                binding.progressDuration.setText(getReadableTime((int) player.getCurrentPosition()));
                binding.seekBar.setProgress((int) player.getCurrentPosition());
            }

            //repeat calling method
            updatePlayerPositionProgress();
        }, 1000);
    }

    private void showCurrentArtwork() {
        binding.artworkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);
        binding.blurBg.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);

        if (binding.artworkView.getDrawable() == null) {
            binding.artworkView.setImageResource(R.drawable.default_albumart);
            binding.blurBg.setImageResource(R.drawable.default_albumart);
        }

    }

    //get total duration text method
    String getReadableTime(int totalDuration) {
        String time;
        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((totalDuration % (1000 * 60 * 60)) % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        if (hrs < 1) {
            time = min + ":" + secs;
        } else {
            time = hrs + ":" + min + ":" + secs;
        }
        return time;
    }
}