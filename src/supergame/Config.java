package supergame;

import java.util.logging.Level;

public class Config {
    public static final int CHUNK_DIVISION = 16;
    public static final int CHUNK_LOAD_DISTANCE = 6;
    public static final int WORKER_THREADS = 2;
    public static final int FRAME_RATE_CAP = 60;

    // CHATTING
    public static int CHAT_HISTORY = 10;
    public static float CHAT_FADE = 10000;
    public static int CHAT_SPACING = 25;

    // NETWORKING
    public static int CHAR_STATE_SAMPLES = 20;
    public static double CORRECTION_WEIGHT = 0.125;
    public static double SAMPLE_DELAY = 100;
    public static int OBJECT_BUFFER_SIZE = 128*1024;
    public static int WRITE_BUFFER_SIZE = OBJECT_BUFFER_SIZE * 4;

    // LOGGING
    public static Level LOG_LEVEL = Level.SEVERE;
}
