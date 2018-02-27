package com.bis.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import net.sf.json.JSONObject;

/**
 * 
 * @ClassName: VisitorDao
 * @Description: 大数据相关dao
 * @author gyr
 * @date 2017年8月9日 下午16:09:12
 *
 */
public interface VisitorDao {
    /**
     * 
     * @Title: saveData
     * @Description: 读取文件解析对象存放数据库
     * @param list
     * @return
     */
    public int saveData(List<JSONObject> list);

    /**
     * 
     * @Title: saveData1
     * @Description: 读取关联文件存放数据库
     * @param list
     * @return
     */
    public int saveData1(List<JSONObject> list);

    /**
     * 
     * @Title: getData
     * @Description: 根据字段统计用户比例
     * @param columnName
     * @return
     */
    public List<Map<String, Object>> getData(@Param("columnName") String columnName,
            @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 
     * @Title: getAge
     * @Description: 获取年龄段比例
     * @param baseNum
     * @return
     */
    public List<Map<String, Object>> getAge(@Param("baseNum") int baseNum, @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    /**
     * 
     * @Title: getDataByShopId
     * @Description: 根据字段统计用户比例,基于店铺
     * @param columnName
     * @param shopId
     * @return
     */
    public List<Map<String, Object>> getDataByShopId(@Param("columnName") String columnName,
            @Param("shopId") String shopId, @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("tableName") String tableName);

    /**
     * 
     * @Title: getAgeByShopId
     * @Description: 获取年龄段比例,基于店铺
     * @param baseNum
     * @param shopId
     * @return
     */
    public List<Map<String, Object>> getAgeByShopId(@Param("baseNum") int baseNum, @Param("shopId") String shopId,
            @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("tableName") String tableName);

    /**
     * 
     * @Title: getDataByCategoryId
     * @Description: 根据字段统计用户比例,基于类别
     * @param columnName
     * @param categoryId
     * @return
     */
    public List<Map<String, Object>> getDataByCategoryId(@Param("columnName") String columnName,
            @Param("categoryId") String categoryId, @Param("startTime") String startTime,
            @Param("endTime") String endTime, @Param("tableName") String tableName, @Param("storeId") String storeId);

    /**
     * 
     * @Title: getAgeByCategoryId
     * @Description: 获取年龄段比例,基于类别
     * @param baseNum
     * @param categoryId
     * @return
     */
    public List<Map<String, Object>> getAgeByCategoryId(@Param("baseNum") int baseNum,
            @Param("categoryId") String categoryId, @Param("startTime") String startTime,
            @Param("endTime") String endTime, @Param("tableName") String tableName, @Param("storeId") String storeId);

    /**
     * 
     * @Title: getDataByStoreId
     * @Description: 根据字段统计用户比例,基于商场
     * @param columnName
     * @param storeId
     * @return
     */
    public List<Map<String, Object>> getDataByStoreId(@Param("columnName") String columnName,
            @Param("storeId") String storeId, @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("tableName") String tableName);

    /**
     * 
     * @Title: getAgeByStoreId
     * @Description: 获取年龄段比例,基于商场
     * @param baseNum
     * @param storeId
     * @return
     */
    public List<Map<String, Object>> getAgeByStoreId(@Param("baseNum") int baseNum, @Param("storeId") String storeId,
            @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("tableName") String tableName);

    /**
     * 
     * @Title: getMapVisitorCount 
     * @Description: 当天楼层客流量排名 
     * @param tableName
     * @param storeId
     * @return
     */
    public List<Map<String, Object>> getMapVisitorCount(@Param("tableName") String tableName,
            @Param("storeId") String storeId);

    /**
     * 
     * @Title: getShopVisitorCount 
     * @Description: 当天店铺客流量排名 
     * @param tableName
     * @param storeId
     * @return
     */
    public List<Map<String, Object>> getShopVisitorCount(@Param("tableName") String tableName,
            @Param("storeId") String storeId);

}
