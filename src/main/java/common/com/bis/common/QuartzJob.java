package com.bis.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.bis.common.conf.Params;
import com.bis.dao.LocationDao;
import com.bis.dao.MapMngDao;
import com.bis.dao.MarketDao;
import com.bis.dao.ShopDao;
import com.bis.dao.StatisticsDao;
import com.bis.model.AreaModel;
import com.bis.model.IvasSvaModel;
import com.bis.model.LocationModel;
import com.bis.model.MarketModel;
import com.bis.model.NewUserModel;
import com.bis.model.ShopModel;
import com.bis.model.SvaModel;
import com.bis.model.TrendMapModel;
import com.bis.model.TrendShopModel;
import com.bis.service.DataAnalysisService;
import com.bis.web.auth.PeripheryService;

public class QuartzJob {

    @Value("${mysql.db}")
    private String db;

    @Autowired
    private StatisticsDao statisticsDao;
    
    @Autowired
    private ShopDao shopDao;

    @Autowired
    private MapMngDao mapMngDao;

    @Autowired
    private MarketDao marketDao;

    @Autowired
    private DataAnalysisService analysisService;

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private SubscriptionService service;

    @Autowired
    private PeripheryService peripheryService;

    @Value("${sva.subscriptionType}")
    private int subscriptionType;
    
    @Value("${sva.subscriptionUrl}")
    private int subscriptionUrl;
    
    @Value("${sva.subivasurl1}")
    private String subivasurl1;
    
    @Value("${sva.subivasurl2}")
    private String subivasurl2;
    
//    @Value("${sva.ftpIp}")
//    private String ftpIp;
//    
//    @Value("${sva.ftpPort}")
//    private int ftpPort;
//    
//    @Value("${sva.ftpUserName}")
//    private String ftpUserName;
//    
//    @Value("${sva.ftpPassWord}")
//    private String ftpPassWord;
//    
//    @Value("${sva.ftpRemotePath}")
//    private String ftpRemotePath;
//    
//    @Value("${sva.ftpFileName}")
//    private String ftpFileName;
//    
//    @Value("${sva.ftpType}")
//    private int ftpType;
    
    @Value("${sva.subTimes}")
    private int subTimes;

    private static final Logger LOG = Logger.getLogger(QuartzJob.class);

    /**
     * @Title: doCreateTable
     * @Description: 动态创建位置数据表任务处理器
     */
    public void doCreateTable() {
        String tableName = "bi_location_" + Util.dateFormat(new Date(), "yyyyMMdd");

        Calendar cal = Calendar.getInstance();
        cal.add(5, 1);
        String time = Util.dateFormat(cal.getTime(), "yyyyMMdd");
        String tableNameTommorrow = "bi_location_" + time;
        try {
            if (statisticsDao.isTableExist(tableName, this.db) < 1) {
                LOG.info("create table today:" + tableName);

                statisticsDao.createTable(tableName);
            }

            if (this.statisticsDao.isTableExist(tableNameTommorrow, this.db) < 1) {
                LOG.info("create table tommorrow:" + tableName);

                statisticsDao.createTable(tableNameTommorrow);
            }
        } catch (Exception e) {
            LOG.error(e);
        }
        // 店铺表
        String nowMouths = Util.dateFormat(new Date(), Params.YYYYMM);
        String shopTableName = Params.SHOPLOCATION + nowMouths;
        try {
            // 创建shop表
            if (statisticsDao.isTableExist(shopTableName, db) < 1) {
                LOG.info("create" + shopTableName);
                // 动态创建表
                statisticsDao.createShopTable(shopTableName);
            }
        } catch (Exception e) {
            LOG.error(e);
        }

    }

    public void doStatistics() {
        // 表名
        String nowDay = Util.dateFormat(new Date(), Params.YYYYMMDD);
        String nowMouth = Util.dateFormat(new Date(), Params.YYYYMM00);
        String nowMouths = Util.dateFormat(new Date(), Params.YYYYMM);
        String tableName = Params.LOCATION + nowDay;
        String shopTableName = Params.SHOPLOCATION + nowMouths;
        // 商场数据集合
        List<Map<String, Object>> list = statisticsDao.getStoreStatistics(tableName);
        LOG.debug("tableName:" + tableName + " siteList:" + list);
        String storeDay = "replace into bi_static_store_delay(time,delaytime,allcount,storeId,type) values";
        String visitTime = null;
        for (Map<String, Object> m : list) {
            int allcount = Integer.parseInt(m.get("allcount").toString());
            int usercount = Integer.parseInt(m.get("usercount").toString());
            int storeId = Integer.parseInt(m.get("storeId").toString());
            visitTime = Util.getMinute(Long.valueOf((allcount * 2000)), usercount);
            storeDay += "('" + nowDay + "','" + visitTime + "','" + usercount + "','" + storeId + "','" + 0 + "'),";
        }
        if (list.size() > 0) {
            storeDay = storeDay.substring(0, storeDay.length() - 1);
            int result = statisticsDao.doUpdate(storeDay);
            LOG.debug("storeDay result:" + result);
        }

        // 查询所有的区域
        List<AreaModel> listAreaData = statisticsDao.getAreaData();
        String userVisitTime = null;
        int areaSize = listAreaData.size();
        LOG.debug("storeDay listAreaDataSize:" + areaSize);
        for (int i = 0; i < areaSize; i++) {
            String userDay = "replace into " + shopTableName + "(time,delaytime,userId,shopId,type) values";
            AreaModel areaModel = listAreaData.get(i);
            List<Map<String, Object>> listArea = statisticsDao.getUserDataByArea(tableName, areaModel);
            int shopId = 0;
            for (Map<String, Object> m : listArea) {
                int allcount = Integer.parseInt(m.get("allcount").toString());
                String userId = m.get("userId").toString();
                shopId = Integer.parseInt(m.get("shopId").toString());
                userVisitTime = Util.getMinute(Long.valueOf((allcount * 2000)), 1);
                userDay += "('" + nowDay + "','" + userVisitTime + "','" + userId + "','" + shopId + "','" + 0 + "'),";
            }
            if (listArea.size() > 0) {
                userDay = userDay.substring(0, userDay.length() - 1);
                int areaResult = statisticsDao.doUpdate(userDay);
                LOG.debug("storeDay result:" + areaResult + " shopId:" + shopId);
            }

        }
        //楼层每天驻留时长人数统计
//        List<String> mapList = mapMngDao.getAllMapId();
        List<Map<String, Object>> maps = statisticsDao.getAllcountByMapId(tableName);
        String userDay = "replace into bi_static_floor (time,delaytime,allcount,mapId) values";
        for (int j = 0; j < maps.size(); j++) {
                String chaMapIds = String.valueOf(maps.get(j).get("mapId"));
                String allcount =  String.valueOf(maps.get(j).get("allcount"));
                int count = Integer.parseInt(String.valueOf(maps.get(j).get("count")));
                String floorVisit = Util.getMinute(Long.valueOf((Integer.parseInt(allcount) * 2000)), count);
                userDay += "('" + nowDay + "','" + floorVisit + "','" + count + "','" + chaMapIds + "'),";
            
        }
        if (maps.size() > 0) {
            userDay = userDay.substring(0, userDay.length() - 1);
            int areaResult = statisticsDao.doUpdate(userDay);
            LOG.debug("floorDay result:" + areaResult);
        }

        // 商场按月统计
        String storeMouth = "replace into bi_static_store_delay(time,delaytime,allcount,storeId,type) values";
        List<Map<String, Object>> storeListMouth = statisticsDao.getStoreByMouth(nowMouth);
        double storeVisitTime = 0;
        for (Map<String, Object> m : storeListMouth) {
            double allTime = Double.valueOf(m.get("allTime").toString());
            int allcount = Integer.parseInt(m.get("allcount").toString());
            int storeId = Integer.parseInt(m.get("storeId").toString());
            storeVisitTime = allTime / allcount;
            storeMouth += "('" + nowMouth + "','" + storeVisitTime + "','" + allcount + "','" + storeId + "','" + 1
                    + "'),";
        }
        if (storeListMouth.size() > 0) {
            storeMouth = storeMouth.substring(0, storeMouth.length() - 1);
            int areaResult = statisticsDao.doUpdate(storeMouth);
            LOG.debug("storeMouth result:" + areaResult);
        }

    }

    /**
     * @Title: calcPeopleRoute
     * @Description: 预处理，获取客户的店铺轨迹信息
     */
    public void calcPeopleRoute() {
        LOG.debug("calcPeopleRoute start:" + new Date().getTime());
        // 今日表
        String nowDay = Util.dateFormat(new Date(), Params.YYYYMMDD);
        String tableName = Params.LOCATION + nowDay;
        analysisService.calcPeopleRoute(tableName);
        LOG.debug("calcPeopleRoute end:" + new Date().getTime());
    }

    public void saveNewPeople() {
        LOG.debug("saveNewPeople start:" + new Date().getTime());
        // 今日表
        String nowDay = Util.dateFormat(new Date(), Params.YYYYMMDD);
        String tableName = Params.LOCATION + nowDay;
        String nowDays = Util.dateFormat(new Date(), Params.YYYYMMDD2);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        String yesDay = Util.dateFormat(c.getTimeInMillis(), Params.YYYYMMDD);
        String yesTableName = Params.LOCATION + yesDay;

        List<ShopModel> list = shopDao.getAllShopData();
        for (int i = 0; i < list.size(); i++) {
            ShopModel model = list.get(i);
            List<String> todayList = new ArrayList<>();
            List<String> yesdayList = new ArrayList<>();
            int newUser = 0;
            int toDayUser = 0;
            int yesDayUser = 0;
            String shopId = model.getId();
            NewUserModel userModel = new NewUserModel();
            todayList = locationDao.getNowAllCount(tableName, model);
            toDayUser = todayList.size();
            yesdayList = locationDao.getNowAllCount(yesTableName, model);
            yesDayUser = yesdayList.size();
            todayList.removeAll(yesdayList);
            newUser = todayList.size();
            userModel.setNewUser(newUser);
            userModel.setShopId(shopId);
            userModel.setTime(nowDays);
            userModel.setTodayUser(toDayUser);
            userModel.setYesdayUser(yesDayUser);
            int result = statisticsDao.saveNewUser(userModel);
            LOG.debug("saveNewPeople shopId:" + shopId + ",reslut:" + result);
        }
        List<MarketModel> marketList = marketDao.getAllStore();
        for (int i = 0; i < marketList.size(); i++) {
            String storeId = marketList.get(i).getId();
            List<String> todayList = new ArrayList<>();
            List<String> yesdayList = new ArrayList<>();
            int newUser = 0;
            int toDayUser = 0;
            int yesDayUser = 0;
            NewUserModel userModel = new NewUserModel();
            todayList = statisticsDao.getStroeUserById(storeId, tableName);
            toDayUser = todayList.size();
            yesdayList = statisticsDao.getStroeUserById(storeId, yesTableName);
            yesDayUser = yesdayList.size();
            todayList.removeAll(yesdayList);
            newUser = todayList.size();
            userModel.setNewUser(newUser);
            userModel.setStoreId(storeId);
            userModel.setTime(nowDays);
            userModel.setTodayUser(toDayUser);
            userModel.setYesdayUser(yesDayUser);
            int result = statisticsDao.saveNewUser(userModel);
            LOG.debug("saveNewPeople storeId:" + storeId + ",reslut:" + result);
        }
        
        List<String> mapIdList = mapMngDao.getAllMapId();
        for (int i = 0; i < mapIdList.size(); i++) {
            String mapId = mapIdList.get(i);
            List<String> todayList = new ArrayList<>();
            List<String> yesdayList = new ArrayList<>();
            int newUser = 0;
            int toDayUser = 0;
            int yesDayUser = 0;
            NewUserModel userModel = new NewUserModel();
            todayList = statisticsDao.getMapIdUser(tableName,mapId);
            toDayUser = todayList.size();
            yesdayList = statisticsDao.getMapIdUser(yesTableName,mapId);
            yesDayUser = yesdayList.size();
            todayList.removeAll(yesdayList);
            newUser = todayList.size();
            userModel.setNewUser(newUser);
            userModel.setMapId(mapId);
            userModel.setTime(nowDays);
            userModel.setTodayUser(toDayUser);
            userModel.setYesdayUser(yesDayUser);
            int result = statisticsDao.saveNewUser(userModel);
            LOG.debug("saveNewPeople floor:" + mapId + ",reslut:" + result); 
        }
        LOG.debug("saveNewPeople end:" + new Date().getTime());
    }

    public void SubscriptionSva() {
        LOG.debug("QuartzJob 订阅类型：" + subscriptionType);
        boolean result = verificationSva();
        if (result) {
            if (subscriptionType==0) {
                Map<String, SvaModel> map = getSvaModel();
                SvaModel nonAnonymousModel = map.get("nonAnonymousModel");
                SvaModel SpecifiesModel = map.get("SpecifiesModel");
                try {
                    if (nonAnonymousModel != null) {
                        for (int i = 0; i <subTimes; i++) {
                            nonAnonymousModel.setId(String.valueOf(i+1));
                            service.subscribeSva(nonAnonymousModel);
                        }
                    }
                    if (SpecifiesModel != null) {
                        SpecifiesModel.setId(String.valueOf(subTimes+1));
                        service.subscribeSva(SpecifiesModel);
                    }
                } catch (Exception e) {
                    LOG.debug("QuartzJob sva 订阅出错：" + e.getMessage());
                }
            } else {
                String baseUrl = null;
                String url1 = subivasurl1;
                String url2 = subivasurl2;
                if (subscriptionUrl==0){
                  baseUrl = url1;
                }else
                {
                    baseUrl = url2;
                }
                String url = null;
                String subUrl = null;
                String sign = null;
                String subSign = null;
                String token = null;
                MySHA256 my = new MySHA256();
                List<IvasSvaModel> list = statisticsDao.getIvasList();
                for (int i = 0; i < list.size(); i++) {
                    SvaModel sva = new SvaModel();
                    IvasSvaModel model = list.get(i);
                    sva.setId(String.valueOf(model.getId()));
                    String myKey = model.getMyKey();
                    String mySecret = model.getMySecret();
                    String getTokenUrl1 = model.getGetTokenUrl();
                    String getTokenUrl = "/api"+model.getGetTokenUrl();
                    String appName = model.getAppName();
                    String appPassWord = model.getAppPassWord();
                    String subscriptionUrl1 = model.getSubscriptionUrl();
                    String subscriptionUrl = "/api"+model.getSubscriptionUrl();
                    sva.setUsername(appName);
                    url = baseUrl+getTokenUrl1;
                    String getTokenParam ="{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\": {\"user\": {\"domain\": \"Api\",\"name\": \""+appName+"\",\"password\": \""+appPassWord+"\"}}},\"iplist\":[]}}";
                    sign = my.JavaSHA256(mySecret+getTokenUrl+getTokenParam).toUpperCase();
                    token = service.getIvasToken(url, myKey, sign, getTokenParam);
                    LOG.debug("QuartzJob ivas getToken:"+token);
                    subUrl = baseUrl +subscriptionUrl1;
                    String subParam =  "{\"APPID\":\"" +appName+"\"}";
//                    JSONObject param = new JSONObject();
//                    param.put("appid",appName);
                    subSign = my.JavaSHA256(mySecret+subscriptionUrl+subParam).toUpperCase();
                    service.subIvasSva(subUrl, myKey, subSign, subParam, token, sva);

                }
            } 
        }

    }

    public void savePeripheryService() {

        peripheryService.readPeripheralService();
    }

    public Map<String, SvaModel> getSvaModel() {
        Map<String, SvaModel> map = new HashMap<String, SvaModel>();
        SvaModel nonAnonymousModel = new SvaModel();
        SvaModel SpecifiesModel = new SvaModel();
        String path = getClass().getResource("/").getPath();
        LOG.debug("SubscriptionSva Filepath:" + path);
        try {
            File f = new File(path + "svaConfigue.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(f);
            NodeList nl2 = doc.getElementsByTagName("Configuration");
            for (int i = 0; i < nl2.getLength(); i++) {
                nonAnonymousModel.setId(doc.getElementsByTagName("id").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel.setIp(doc.getElementsByTagName("ip").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel
                        .setTokenPort(doc.getElementsByTagName("token_port").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel
                        .setBrokerPort(doc.getElementsByTagName("broker_port").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel.setUsername(doc.getElementsByTagName("name").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel
                        .setPassword(doc.getElementsByTagName("password").item(i).getFirstChild().getNodeValue());
                nonAnonymousModel.setStatus(
                        Integer.parseInt(doc.getElementsByTagName("status").item(i).getFirstChild().getNodeValue()));
                nonAnonymousModel.setType(Integer
                        .parseInt(doc.getElementsByTagName("subscribe_type").item(i).getFirstChild().getNodeValue()));
                nonAnonymousModel.setIdType(doc.getElementsByTagName("ip_type").item(i).getFirstChild().getNodeValue());
                // 指定用户信息
                SpecifiesModel.setId(doc.getElementsByTagName("id1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel.setIp(doc.getElementsByTagName("ip1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel
                        .setTokenPort(doc.getElementsByTagName("token_port1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel
                        .setBrokerPort(doc.getElementsByTagName("broker_port1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel.setUsername(doc.getElementsByTagName("name1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel
                        .setPassword(doc.getElementsByTagName("password1").item(i).getFirstChild().getNodeValue());
                SpecifiesModel.setStatus(
                        Integer.parseInt(doc.getElementsByTagName("status1").item(i).getFirstChild().getNodeValue()));
                SpecifiesModel.setType(Integer
                        .parseInt(doc.getElementsByTagName("subscribe_type1").item(i).getFirstChild().getNodeValue()));
                SpecifiesModel.setIdType(doc.getElementsByTagName("ip_type1").item(i).getFirstChild().getNodeValue());
            }

        } catch (Exception e) {
            LOG.debug("QuartzJob:解析配置出错!");
        }
        map.put("nonAnonymousModel", nonAnonymousModel);
        map.put("SpecifiesModel", SpecifiesModel);
        return map;
    }

    /**
     * 
     * @Title: getTrend
     * @Description: 楼层和店铺关联动向统计预处理
     */
    public void getTrend() {
        // System.out.println("楼层和店铺动向预处理调用了...");
        try {
            
            LOG.info("QuartzJob ~ getTrend 楼层和店铺关联动向预处理");
            Calendar calendar = Calendar.getInstance();
            // 防止跑到下一天，往前推30分钟取时间值
            calendar.add(Calendar.MINUTE, -30);
            Date nowDate = calendar.getTime();
            String nowDay = Util.dateFormat(calendar.getTimeInMillis(), Params.YYYYMMDD);
            int hour = calendar.get(Calendar.HOUR_OF_DAY) + 1;
            String tableName = Params.LOCATION + nowDay;
            // 根据楼层计算动向预处理
            List<String> mapIdList = mapMngDao.getAllMapId();
            for (String mapId : mapIdList) {
                // 用户来源map
                Map<String, Integer> trendMap = new HashMap<>();
                List<String> userIdList = locationDao.queryAllUserIdByMapId(mapId, tableName);
                for (String userId : userIdList) {
                    LocationModel model = locationDao.getOtherMapIdByMaxTime(mapId, userId, tableName);
                    if (model == null) {
                        if (trendMap.get("other") == null) {
                            trendMap.put("other", 1);
                        } else {
                            trendMap.put("other", trendMap.get("other") + 1);
                        }
                    } else {
                        String resultMapId = model.getMapId();
                        if (trendMap.get(resultMapId) == null) {
                            trendMap.put(resultMapId, 1);
                        } else {
                            trendMap.put(resultMapId, trendMap.get(resultMapId) + 1);
                        }
                    }
                }
                TrendMapModel trendMapModel = new TrendMapModel(mapId, hour, nowDate);
                // 按小时保存
                for (Map.Entry<String, Integer> entry : trendMap.entrySet()) {
                    if ("other".equals(entry.getKey())) {
                        trendMapModel.setFromMapId(null);
                    } else {
                        trendMapModel.setFromMapId(entry.getKey());
                    }
                    trendMapModel.setVisitorCount(entry.getValue());
                    locationDao.saveTrendMapByHour(trendMapModel);
                }
                // 如果hour为24同时按日保存
                if (hour == 24) {
                    int day = calendar.get(Calendar.DATE);
                    trendMapModel.setSign(day);
                    for (Map.Entry<String, Integer> entry : trendMap.entrySet()) {
                        if ("other".equals(entry.getKey())) {
                            trendMapModel.setFromMapId(null);
                        } else {
                            trendMapModel.setFromMapId(entry.getKey());
                        }
                        trendMapModel.setVisitorCount(entry.getValue());
                        locationDao.saveTrendMapByDay(trendMapModel);
                    }
                }
            }
            // 根据店铺计算动向预处理
            List<Integer> shopIdList = shopDao.getAllShopId();
            for (int shopId : shopIdList) {
                Map<Integer, Integer> trendMap = new HashMap<>(); // 用户来源map
                List<String> userIdList = locationDao.queryAllUserIdByShopId(shopId, tableName);
                for (String userId : userIdList) {
                    LocationModel model = locationDao.getOtherShopIdByMaxTime(shopId, userId, tableName);
                    if (model == null) {
                        if (trendMap.get(-1) == null) {
                            trendMap.put(-1, 1);
                        } else {
                            trendMap.put(-1, trendMap.get(-1) + 1);
                        }
                    } else {
                        int resultMapId = model.getId();
                        if (trendMap.get(resultMapId) == null) {
                            trendMap.put(resultMapId, 1);
                        } else {
                            trendMap.put(resultMapId, trendMap.get(resultMapId) + 1);
                        }
                    }
                }
                TrendShopModel trendShopModel = new TrendShopModel(shopId, hour, nowDate);
                // 按小时保存
                for (Map.Entry<Integer, Integer> entry : trendMap.entrySet()) {
                    trendShopModel.setFromShopId(entry.getKey());
                    trendShopModel.setVisitorCount(entry.getValue());
                    locationDao.saveTrendShopByHour(trendShopModel);
                }
                // 如果hour为24同时按日保存
                if (hour == 24) {
                    int day = calendar.get(Calendar.DATE);
                    trendShopModel.setSign(day);
                    for (Map.Entry<Integer, Integer> entry : trendMap.entrySet()) {
                        trendShopModel.setFromShopId(entry.getKey());
                        trendShopModel.setVisitorCount(entry.getValue());
                        locationDao.saveTrendShopByDay(trendShopModel);
                    }
                }
            }
            

        } catch (Exception e) {
            System.err.println(e);
        }   
    }
    
    /** 
     * @Title: verificationSva 
     * @Description: sva验证
     * @return 
     */
    private boolean verificationSva()
    {
        long nowTime = System.currentTimeMillis()-120000;
        String tableName = Params.LOCATION + Util.dateFormat(new Date(), "yyyyMMdd");
        int count = 0;
        count = locationDao.getCountByTimestamp(nowTime, tableName);
        boolean result = false;
        if (count==0) {
            result = true;
        }
        LOG.debug("verificationSva result:"+result);
        return result;
    }
//    
//    public void doFtpData()
//    {
//      String localPath = getClass().getResource("/").getPath();
////      String localPath = System.getProperty("user.dir");
////      localPath = localPath.substring(0, localPath.indexOf("bin"))+"webapps/SVAProject/WEB-INF";
//      localPath = localPath.substring(1, localPath.indexOf("/classes"));
//      localPath = localPath + File.separator + "ftp" + File.separator;
//     LOG.debug("doFtpData localPath"+localPath);
//     boolean ftpResult = Util.downFtpFile(ftpIp,ftpPort, ftpUserName, ftpPassWord, ftpRemotePath, ftpFileName, localPath, ftpType);
//     if (ftpResult) {
//         Date date = new Date();
//         String time = Util.dateFormat(date, Params.YYYYMMDDHHMMSS);
//         String filePath = localPath;
//         File file = new File(filePath, "visitors.txt");
//         if (file.exists()) {
//             System.out.println("信息文件存在");
//         }
//         List<JSONObject> list = new ArrayList<>();
//         String[] names = Params.VISITOR_COLUMNS;
//         BufferedReader reader = null;
//         try {
//             reader = new BufferedReader(new FileReader(file));
//             String tempString = null;
//             // 一次读入一行，直到读入null为文件结束
//             while ((tempString = reader.readLine()) != null) {
//                 JSONObject jsonObject = new JSONObject();
//                 String[] values = tempString.replace("|", "_").split("_");
//                 for (int i = 0; i < names.length; i++) {
//                     jsonObject.put(names[i], values[i + 1]);
//                 }
//                 jsonObject.put("time", time);
//                 list.add(jsonObject);
//             }
//             reader.close();
//         } catch (IOException e) {
//             e.printStackTrace();
//         } finally {
//             if (reader != null) {
//                 try {
//                     reader.close();
//                 } catch (IOException e1) {
//                 }
//             }
//         }
//         int num = dao.saveData(list);
//         LOG.debug("VisitorController~插入Visitor数据条数:" + num);
//
//         String filePath1 = localPath;
//         File file1 = new File(filePath1, "relevance.txt");
//         if (file1.exists()) {
//             System.out.println("关联文件存在");
//         }
//         List<JSONObject> list1 = new ArrayList<>();
//         String[] names1 = { "userId", "imsi", "eNBid", "cellId" };
//         BufferedReader reader1 = null;
//         try {
//             reader1 = new BufferedReader(new FileReader(file1));
//             String tempString = null;
//             // 一次读入一行，直到读入null为文件结束
//             while ((tempString = reader1.readLine()) != null) {
//                 JSONObject jsonObject1 = new JSONObject();
//                 String[] values1 = tempString.replace("|", "_").split("_");
//                 for (int i = 0; i < names1.length; i++) {
//                     jsonObject1.put(names1[i], values1[i + 1]);
//                 }
//                 list1.add(jsonObject1);
//             }
//             reader1.close();
//         } catch (IOException e) {
//             e.printStackTrace();
//         } finally {
//             if (reader1 != null) {
//                 try {
//                     reader1.close();
//                 } catch (IOException e1) {
//                 }
//             }
//             Util.deleteAll(new File(localPath));
//         }
//         dao.saveData1(list1);
//    }else
//    {
//        LOG.debug("doFtpData downFtpFile failed result"+ftpResult);
//    }
//    }
}
