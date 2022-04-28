package com.droidsee.speechrecodemo.Utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class FFmpegUtil {

    public static final int FFMPEG_SUCCESS = 0;
    public static final int FFMPEG_FAILURE = 1;
    public static final int FFMPEG_START = 2;
    public static final int FFMPEG_FINISH = 3;
    public static final int FFMPEG_PROGRESS = 4;

    private static FFmpeg fFmpeg;
    public static void transformToWav(Context context, String filePath, String outPath, Handler handler) throws FFmpegNotSupportedException, FFmpegCommandAlreadyRunningException {
        if(fFmpeg == null) {
            FFmpeg fFmpeg = FFmpeg.getInstance(context);
            fFmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    handler.sendEmptyMessage(FFMPEG_FAILURE);
                }

                @Override
                public void onSuccess() {
//                    handler.sendEmptyMessage(FFMPEG_SUCCESS);
                }

                @Override
                public void onStart() {
//                    handler.sendEmptyMessage(FFMPEG_START);
                }

                @Override
                public void onFinish() {
//                    handler.sendEmptyMessage(FFMPEG_FINISH);
                    if(fFmpeg == null) {
                        return;
                    }

//                    String[] cmd = new String[1];
//                    cmd[0] = "ffmpeg -i " + filePath + " -ab 16 -ar 16000 -ac 1 " + outPath;
                    String[] cmd = new String[]{"-i", filePath, "-ab", "16", "-ar", "16000", "-ac", "1", outPath};
                    try {
                        fFmpeg.execute(cmd, new FFmpegExecuteResponseHandler() {
                            @Override
                            public void onSuccess(String message) {
                                Message msg = new Message();
                                msg.what = FFMPEG_SUCCESS;
                                msg.obj = outPath;
                                handler.sendMessage(msg);
                            }

                            @Override
                            public void onProgress(String message) {
                                Message msg = new Message();
                                msg.what = FFMPEG_PROGRESS;
                                msg.obj = message;
                                handler.sendMessage(msg);
                            }

                            @Override
                            public void onFailure(String message) {
                                Message msg = new Message();
                                msg.what = FFMPEG_FAILURE;
                                msg.obj = message;
                                handler.sendMessage(msg);
                            }

                            @Override
                            public void onStart() {
                                handler.sendEmptyMessage(FFMPEG_START);
                            }

                            @Override
                            public void onFinish() {
                                handler.sendEmptyMessage(FFMPEG_FINISH);
                            }
                        });
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            //ffmpeg -i test.wav -acodec libamr_nb -ab 12.2k -ar 8000 -ac 1 wav2amr.amr
//        -ab bitrate 设置音频码率
//        -ar freq 设置音频采样率
//        -ac channels 设置通道 缺省为1
//        -an 不使能音频纪录
//        -acodec codec 使用codec编解码

//        String[] cmd = new String[6];
//        cmd[0] = "ffmpeg -i";//input
//        cmd[1] = uri;
//        cmd[2] = "-ab 16";
//        cmd[3] = "-ar 16000";
//        cmd[4] = "-ac 1";
//        cmd[5] = outPath;
            String[] cmd = new String[]{"-i", filePath, "-ab", "16", "-ar", "16000", "-ac", "1", outPath};
            try {
                fFmpeg.execute(cmd, new FFmpegExecuteResponseHandler() {
                    @Override
                    public void onSuccess(String message) {
                        Message msg = new Message();
                        msg.what = FFMPEG_SUCCESS;
                        msg.obj = outPath;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onProgress(String message) {
                        Message msg = new Message();
                        msg.what = FFMPEG_PROGRESS;
                        msg.obj = message;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onFailure(String message) {
                        Message msg = new Message();
                        msg.what = FFMPEG_FAILURE;
                        msg.obj = message;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onStart() {
                        handler.sendEmptyMessage(FFMPEG_START);
                    }

                    @Override
                    public void onFinish() {
                        handler.sendEmptyMessage(FFMPEG_FINISH);
                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {
                e.printStackTrace();
            }
        }
    }

}
