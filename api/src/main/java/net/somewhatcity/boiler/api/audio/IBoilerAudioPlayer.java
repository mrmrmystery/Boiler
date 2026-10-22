package net.somewhatcity.boiler.api.audio;

public interface IBoilerAudioPlayer {
    void play(byte[] samples);
    void stop();
    int getAudioQueueSize();
}
