package com.droidsee.speechrecodemo;

public class ApiRequestManager {

    private static ApiRequestManager mInstance;
    public static ApiRequestManager getInstance() {
        if(mInstance == null) {
            mInstance = new ApiRequestManager();
        }
        return mInstance;
    }

    static String GET_TOKEN_URL = "https://[region].api.cognitive.microsoft.com/sts/v1.0/issuetoken";
    static String POST_URL = "https://[region].stt.speech.microsoft.com/speech/recognition/conversation/" +
            "cognitiveservices/v1?language=[language]&profanity=raw";

//    static AsyncTask task = new AsyncTask() {
//        @Override
//        protected Object doInBackground(Object[] objects) {
//            String url = GET_TOKEN_URL.replace("[region]", SpeechRegion);
//            HashMap<String, String> param = new HashMap<>();
//            param.put("Ocp-Apim-Subscription-Key", SpeechSubscriptionKey);
//            String response = HttpPost.getInstance().postData(url, null, param);
//            accessToken = response;
//            lastTokenTime = System.currentTimeMillis();
//            Log.e("get_token", response);
//            System.out.println(lastTokenTime);
//            return null;
//        }
//    };
}
