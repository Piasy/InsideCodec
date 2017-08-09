package com.github.piasy.mediacodecrctest;

import com.google.auto.value.AutoValue;

/**
 * Created by Piasy{github.com/Piasy} on 01/08/2017.
 */

@AutoValue
public abstract class Config {
    public static Builder builder() {
        return new AutoValue_Config.Builder();
    }

    public abstract boolean updateBr();

    public abstract boolean asyncEnc();

    public abstract int initBr();

    public abstract int brStep();

    public abstract int quality();

    public abstract int brMode();

    public abstract int outputWidth();

    public abstract int outputHeight();

    public abstract int outputFps();

    public abstract int outputKeyFrameInterval();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder updateBr(boolean updateBr);

        public abstract Builder asyncEnc(boolean asyncEnc);

        public abstract Builder initBr(int initBr);

        public abstract Builder brStep(int brStep);

        public abstract Builder quality(int quality);

        public abstract Builder brMode(int brMode);

        public abstract Builder outputWidth(int outputWidth);

        public abstract Builder outputHeight(int outputHeight);

        public abstract Builder outputFps(int outputFps);

        public abstract Builder outputKeyFrameInterval(int outputKeyFrameInterval);

        public abstract Config build();
    }
}
