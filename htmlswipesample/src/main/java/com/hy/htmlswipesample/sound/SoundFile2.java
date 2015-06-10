package com.hy.htmlswipesample.sound;

/**
 * Created by lim2621 on 2015-05-14.
 */

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class SoundFile2 {
    private File mInputFile = null;

    // Member variables representing frame data
    private String mFileType;
    private int mFileSize;
    private int mAvgBitRate;  // Average bit rate in kbps.
    private int mSampleRate;
    private int mChannels;
    private int mNumSamples;  // total number of samples per channel in audio file
    private ByteBuffer mDecodedBytes;  // Raw audio data
    private ShortBuffer mDecodedSamples;  // shared buffer with mDecodedBytes.

    static private int mNumFrames = 0;
    static private int mStaticWidth = 0;
    private int[] mFrameGains;
    private int[] mFrameLens;
    private int[] mFrameOffsets;
    private int test=0;

    private String[] checkList = {"mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "ogg"};


    public SoundFile2(String path, int width) throws IOException, InvalidInputException {
        mNumFrames = width;

        File f = new File(path);
        if (!f.exists()) {

        }
        String name = f.getName().toLowerCase();
        String[] components = name.split("\\.");
        ReadFile(f);

    }


    // Custom exception for invalid inputs.
    public class InvalidInputException extends Exception {
        // Serial version ID generated by Eclipse.
        private static final long serialVersionUID = -2505698991597837165L;

        public InvalidInputException(String message) {
            super(message);
        }
    }

    // TODO(nfaralli): what is the real list of supported extensions? Is it device dependent?
    private String[] getSupportedExtensions() {
        return new String[]{"mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "ogg"};
    }

    public boolean isFilenameSupported(String filename) {
        String[] extensions = getSupportedExtensions();
        for (int i = 0; i < extensions.length; i++) {
            if (filename.endsWith("." + extensions[i])) {
                return true;
            }
        }
        return false;
    }

    public static void setmNumFrames(int mNumFrames) {
        SoundFile2.mNumFrames = mNumFrames;
    }

    public static int getmNumFrames() {
        return mNumFrames;
    }


    public String getFiletype() {
        return mFileType;
    }

    public int getFileSizeBytes() {
        return mFileSize;
    }

    public int getAvgBitrateKbps() {
        return mAvgBitRate;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getChannels() {
        return mChannels;
    }

    public int getNumSamples() {
        return mNumSamples;  // Number of samples per channel.
    }

    // Should be removed when the app will use directly the samples instead of the frames.
    public int getNumFrames() {
        return mNumFrames;
    }

    // Should be removed when the app will use directly the samples instead of the frames.
    public int getSamplesPerFrame() {
        return mNumFrames;  // just a fixed value here...
    }

    public int setSamplesPerFrame(int x) {
        return mNumFrames;  // 비율따라 틀려서
    }

    // Should be removed when the app will use directly the samples instead of the frames.
    public int[] getFrameGains() {
        return mFrameGains;
    }

    public ShortBuffer getSamples() {
        if (mDecodedSamples != null) {
            return mDecodedSamples.asReadOnlyBuffer();
        } else {
            return null;
        }
    }


    private void ReadFile(File inputFile)
            throws java.io.FileNotFoundException,
            IOException, InvalidInputException {
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format = null;
        int i;

        mInputFile = inputFile;
        String[] components = mInputFile.getPath().split("\\.");
        mFileType = components[components.length - 1];
        mFileSize = (int) mInputFile.length();
        extractor.setDataSource(mInputFile.getPath());
        int numTracks = extractor.getTrackCount();
        // find and select the first audio track present in the file.
        for (i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);
                break;
            }
        }
        if (i == numTracks) {
            throw new InvalidInputException("No audio track found in " + mInputFile);
        }
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, null, null, 0);
        codec.start();

        int decodedSamplesSize = 0;  // size of the output buffer containing decoded samples.
        byte[] decodedSamples = null;
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        int sample_size;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long presentation_time;
        int tot_size_read = 0;
        boolean done_reading = false;

        // 디코딩 된 샘플의 크기가 버퍼에 설정 1메가바이트 // (~는 44.1kHz에서 스테레오 스트림의 6 초).
        // 이상 스트림의 경우, 버퍼 크기는 대략적를 계산 이후에 증가 될
        // 버퍼의 크기를 조정하기 위하여 모든 샘플들을 저장하는 데 필요한 전체 크기의 추정치
        // 한 번만.
        mDecodedBytes = ByteBuffer.allocate(1 << 20);

        while (true) {
            // read data from file and feed it to the decoder input buffers.
            i=i++;
            Log.i("test","test1"+i);
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
                if (sample_size < 0) {
                    // All samples have been read.
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    presentation_time = extractor.getSampleTime();
                    codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);
                    extractor.advance();
                    tot_size_read += sample_size;
                }
            }

            // 디코더 출력 버퍼에서 디코딩 된 스트림을 가져옵니다.
            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0) {
                if (decodedSamplesSize < info.size) {
                    decodedSamplesSize = info.size;
                    decodedSamples = new byte[decodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                // Check if buffer is big enough. Resize it if it's too small.
                if (mDecodedBytes.remaining() < info.size) {
                    // Getting a rough estimate of the total size, allocate 20% more, and
                    // make sure to allocate at least 5MB more than the initial size.
                    //  전체 크기의 대략적인 견적을 얻기 20 % 이상을 할당하고,
                    // 초기 크기보다 적어도 5메가바이트 이상을 할당 할 수 있는지 확인하십시오.
                    int position = mDecodedBytes.position();
                    int newSize = (int) ((position * (1.0 * mFileSize / tot_size_read)) * 1.2);
                    if (newSize - position < info.size + 5 * (1 << 20)) {
                        newSize = position + info.size + 5 * (1 << 20);
                    }
                    ByteBuffer newDecodedBytes = null;
                    //메모리를 할당하려고합니다. 우리가 OOM을 경우, 가비지 컬렉터를 실행 해보십시오.
                    int retry = 10;
                    while (retry > 0) {
                        i=i++;
                        Log.i("test","test2"+i);
                        try {
                            newDecodedBytes = ByteBuffer.allocate(newSize);
                            break;
                        } catch (OutOfMemoryError oome) {
                            // setting android:largeHeap="true" in <application> seem to help not
                            // reaching this section.
                            retry--;
                        }
                    }
                    if (retry == 0) {
                        // ... 메모리를 할당 더 많은 데이터를 읽고 중지를 완료 할 수 없습니다
                        // 지금까지 디코딩 된 데이터 인스턴스입니다.
                        break;
                    }
                    //ByteBuffer newDecodedBytes = ByteBuffer.allocate(newSize);
                    mDecodedBytes.rewind();
                    newDecodedBytes.put(mDecodedBytes);
                    mDecodedBytes = newDecodedBytes;
                    mDecodedBytes.position(position);
                }
                mDecodedBytes.put(decodedSamples, 0, info.size);
                codec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 이후의 데이터는 새로운 형식을 준수합니다.
                // 우리는 새로운 출력 형식입니다 codec.getOutputFormat ()를 확인할 수 있습니다

            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // 우리는 디코더의 모든 디코딩 된 데이터를 얻었다.
                break;
            }
        }
        mNumSamples = mDecodedBytes.position() / (mChannels * 2);  // One sample = 2 bytes.
        mDecodedBytes.rewind();
        mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
        mDecodedSamples = mDecodedBytes.asShortBuffer();
        mAvgBitRate = (int) ((mFileSize * 8) * ((float) mSampleRate / mNumSamples) / 1000);

        extractor.release();
        extractor = null;
        codec.stop();
        codec.release();
        codec = null;

        // Temporary hack to make it work with the old version.
        mNumFrames = getSamplesPerFrame();
        Log.i("wavecheck", "mNumFrames" + mNumFrames + ",mNumSamples" + mNumSamples + ",getSamplesPerFrame()" + getSamplesPerFrame());
        mFrameGains = new int[mNumFrames];
        mFrameLens = new int[mNumFrames];
        mFrameOffsets = new int[mNumFrames];
        int j;
        int gain, value;
        int frameLens = (int) ((1000 * mAvgBitRate / 8) *
                ((float) getSamplesPerFrame() / mSampleRate));
        int sampleValue = (int) (mNumSamples / mNumFrames); // (전체/넓이)
        for (i = 0; i < mNumFrames; i++) {
            gain = mDecodedSamples.get(i*sampleValue);
            mFrameGains[i] = (int) Math.sqrt(gain);  // here gain = sqrt(max value of 1st channel)...
            mFrameLens[i] = frameLens;  // totally not accurate...
            mFrameOffsets[i] = (int) (i * (1000 * mAvgBitRate / 8) *  //  = i * frameLens
                    ((float) getSamplesPerFrame() / mSampleRate));
        }
        mDecodedSamples.rewind();
    }



}
