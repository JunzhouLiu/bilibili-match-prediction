package top.misec.task;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URLEncodedUtils;
import top.misec.apiquery.ApiList;
import top.misec.apiquery.oftenAPI;
import top.misec.login.Verify;
import top.misec.utils.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Log4j2
public class MatchGame implements Task {

    @Override
    public void run() throws UnsupportedEncodingException, InterruptedException {

        if(oftenAPI.getCoinBalance()<30){
            log.info("30个硬币都没有，参加什么预测呢？任务结束");
            return;
        }
        int pn=1;
        int ps=50;
        String gid="";
        String sids="";
        String today=getTime();
        //String today="2021-03-20";
        log.info(today);
        String urlParam="?pn="+pn+
                "&ps="+ps
                + "&gid="+gid
                + "&sids="+sids
                + "&stime="+today+URLEncoder.encode(" 00:00:00")
                + "&etime="+today+URLEncoder.encode(" 23:59:59")
                + "&pn="+pn
                + "&ps="+ps
                + "&stime="+today+"+00:00:00"
                + "&etime="+today+"+23:59:59";

        JsonObject resultJson=HttpUtil.doGet(ApiList.queryQuestions+urlParam);
        JsonObject jsonObject= resultJson.get("data").getAsJsonObject();
        if(resultJson.get("code").getAsInt()==0){
            JsonArray list= jsonObject.get("list").getAsJsonArray();
            JsonObject pageinfo=jsonObject.get("page").getAsJsonObject();

            if (list != null) {
                int coinNumber=5;
                int contsetId;
                String contestName;
                int questionId;
                String questionTitle;
                int teamId;
                String teamName;
                int seasonId;
                String seasonName;

                for (JsonElement listinfo : list) {
                    log.info("-----预测开始-----");
                    JsonObject contestJson= listinfo.getAsJsonObject().getAsJsonObject("contest");
                    JsonObject questionJson= listinfo.getAsJsonObject().getAsJsonArray("questions")
                            .get(0).getAsJsonObject();
                    contsetId=contestJson.get("id").getAsInt();
                    contestName=contestJson.get("game_stage").getAsString();
                    questionId=questionJson.get("id").getAsInt();
                    questionTitle=questionJson.get("title").getAsString();
                    seasonId=contestJson.get("season").getAsJsonObject()
                            .get("id").getAsInt();
                    seasonName=contestJson.get("season").getAsJsonObject()
                            .get("title").getAsString();

                    log.info(seasonName+" "+contestName+":"+questionTitle);

                    if(questionJson.get("is_guess").getAsInt()==1){
                        log.info("此问题已经参与过预测了，无需再次预测");
                        continue;
                    }
                    JsonObject teamA=questionJson.get("details").getAsJsonArray().get(0).getAsJsonObject();
                    JsonObject teamB=questionJson.get("details").getAsJsonArray().get(1).getAsJsonObject();


                    log.info("当前赔率为:  {}:{}",teamA.get("odds").getAsDouble(),teamB.get("odds").getAsDouble());
                    if(teamA.get("odds").getAsDouble()>=teamB.get("odds").getAsDouble()){
                        teamId=teamB.get("detail_id").getAsInt();
                        teamName=teamB.get("option").getAsString();
                    }else{
                        teamId=teamA.get("detail_id").getAsInt();
                        teamName=teamA.get("option").getAsString();
                    }

                    log.info("拟预测的队伍是:{},预测硬币数为:{}",teamName,coinNumber);
                    doPrediction(contsetId,questionId,teamId,coinNumber);
                    taskSuspend();

                }
            }

        }else{
            log.info("获取赛事信息失败");
        }

    }

    private void taskSuspend() throws InterruptedException {
        Random random = new Random();
        int sleepTime = (int) ((random.nextDouble() + 0.5) * 3000);
        log.info("-----随机暂停{}ms-----\n", sleepTime);
        Thread.sleep(sleepTime);
    }
    private void doPrediction(int oid,int main_id,int detail_id,int count){
        String requestbody="oid="+oid+
                "&main_id="+main_id
                +"&detail_id="+detail_id
                +"&count="+count
                +"&is_fav=0"
                +"&csrf="+ Verify.getInstance().getBiliJct();

        JsonObject result=HttpUtil.doPost(ApiList.doAdd,requestbody);

        if(result.get("code").getAsInt()!=0){
            log.info(result.get("message").getAsString());
        }else {
            log.info("预测成功");
        }

    }

    private String getTime() {
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String time = sdf.format(d);
        return time;
    }

    @Override
    public String getName() {
        return "赛事预测";
    }
}
