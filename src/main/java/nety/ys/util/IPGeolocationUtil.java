package nety.ys.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nety.ys.TokenAuthMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * IP地址地理位置解析工具类
 * 用于通过ip-api.com获取IP地址的地理位置信息
 * 
 * @author nety.ys
 */
public class IPGeolocationUtil {
    
    private static final String API_URL = "http://208.95.112.1/json/";
    private static final String LANGUAGE_PARAM = "?lang=zh-CN";
    private static final Gson gson = new Gson();
    
    // 连接和读取超时时间（毫秒）
    private static final int CONNECT_TIMEOUT = 3000; // 3秒连接超时
    private static final int READ_TIMEOUT = 5000;    // 5秒读取超时
    private static final int MAX_RETRIES = 2;        // 最大重试次数
    
    /**
     * IP地理位置信息数据类
     */
    public static class GeoLocationInfo {
        private final String status;
        private final String message;
        private final String country;
        private final String regionName;
        private final String city;
        private final String isp;
        private final String org;
        private final String query;
        
        public GeoLocationInfo(String status, String message, String country, String regionName, 
                              String city, String isp, String org, String query) {
            this.status = status;
            this.message = message;
            this.country = country;
            this.regionName = regionName;
            this.city = city;
            this.isp = isp;
            this.org = org;
            this.query = query;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getCountry() { return country; }
        public String getRegionName() { return regionName; }
        public String getCity() { return city; }
        public String getIsp() { return isp; }
        public String getOrg() { return org; }
        public String getQuery() { return query; }
        
        /**
         * 获取完整的位置信息字符串
         * 
         * @return 格式化的位置信息
         */
        public String getFullLocation() {
            if (!"success".equals(status)) {
                return "未知位置 (" + message + ")";
            }
            
            StringBuilder location = new StringBuilder();
            if (country != null && !country.isEmpty()) {
                location.append(country);
            }
            if (regionName != null && !regionName.isEmpty()) {
                if (location.length() > 0) location.append(" ");
                location.append(regionName);
            }
            if (city != null && !city.isEmpty()) {
                if (location.length() > 0) location.append(" ");
                location.append(city);
            }
            
            return location.length() > 0 ? location.toString() : "未知位置";
        }
    }
    
    /**
     * 获取IP地址的地理位置信息
     * 
     * @param ipAddress IP地址
     * @return 地理位置信息
     */
    public static GeoLocationInfo getGeoLocation(InetAddress ipAddress) {
        return getGeoLocation(ipAddress.getHostAddress());
    }
    
    /**
     * 获取IP地址的地理位置信息
     * 
     * @param ipAddress IP地址字符串
     * @return 地理位置信息
     */
    public static GeoLocationInfo getGeoLocation(String ipAddress) {
        return getGeoLocationWithRetry(ipAddress, 0);
    }
    
    /**
     * 带重试机制的地理位置获取
     *
     * @param ipAddress IP地址字符串
     * @param retryCount 当前重试次数
     * @return 地理位置信息
     */
    private static GeoLocationInfo getGeoLocationWithRetry(String ipAddress, int retryCount) {
        try {
            DebugLogger.email("正在获取IP {} 的地理位置信息（第{}次尝试）...", ipAddress, retryCount + 1);
            
            // 构建请求URL
            String requestUrl = API_URL + ipAddress + LANGUAGE_PARAM;
            URL url = new URL(requestUrl);
            
            // 发送HTTP请求
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "TokenAuth-Mod/1.1.5");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // 读取响应
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                DebugLogger.email("IP地理位置API响应: {}", response.toString());
                
                // 解析JSON响应
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                
                // 提取信息
                String status = jsonObject.has("status") ? jsonObject.get("status").getAsString() : "fail";
                String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "未知错误";
                String country = jsonObject.has("country") ? jsonObject.get("country").getAsString() : "";
                String regionName = jsonObject.has("regionName") ? jsonObject.get("regionName").getAsString() : "";
                String city = jsonObject.has("city") ? jsonObject.get("city").getAsString() : "";
                String isp = jsonObject.has("isp") ? jsonObject.get("isp").getAsString() : "";
                String org = jsonObject.has("org") ? jsonObject.get("org").getAsString() : "";
                String query = jsonObject.has("query") ? jsonObject.get("query").getAsString() : ipAddress;
                
                DebugLogger.email("IP {} 地理位置信息获取成功: {}", ipAddress,
                    new GeoLocationInfo(status, message, country, regionName, city, isp, org, query).getFullLocation());
                
                return new GeoLocationInfo(status, message, country, regionName, city, isp, org, query);
            } else {
                TokenAuthMod.LOGGER.warn("IP地理位置API请求失败，响应码: {}", responseCode);
                return handleRetry(ipAddress, retryCount, "API请求失败，响应码: " + responseCode);
            }
        } catch (IOException e) {
            TokenAuthMod.LOGGER.error("获取IP地理位置信息时出错", e);
            return handleRetry(ipAddress, retryCount, "网络错误: " + e.getMessage());
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("解析IP地理位置信息时出错", e);
            return new GeoLocationInfo("fail", "解析错误: " + e.getMessage(), "", "", "", "", "", ipAddress);
        }
    }
    
    /**
     * 处理重试逻辑
     *
     * @param ipAddress IP地址
     * @param retryCount 当前重试次数
     * @param errorMessage 错误消息
     * @return 地理位置信息
     */
    private static GeoLocationInfo handleRetry(String ipAddress, int retryCount, String errorMessage) {
        if (retryCount < MAX_RETRIES) {
            DebugLogger.email("IP {} 地理位置获取失败，{}ms后进行第{}次重试...",
                ipAddress, 1000 * (retryCount + 1), retryCount + 2);
            
            try {
                Thread.sleep(1000 * (retryCount + 1)); // 递增延迟
                return getGeoLocationWithRetry(ipAddress, retryCount + 1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return new GeoLocationInfo("fail", "重试被中断: " + errorMessage, "", "", "", "", "", ipAddress);
            }
        } else {
            TokenAuthMod.LOGGER.warn("IP {} 地理位置获取失败，已达到最大重试次数: {}", ipAddress, MAX_RETRIES + 1);
            return new GeoLocationInfo("fail", errorMessage + " (已重试" + MAX_RETRIES + "次)", "", "", "", "", "", ipAddress);
        }
    }
    
    /**
     * 获取当前中国大陆时间的格式化字符串
     * 
     * @return 格式化的时间字符串 (YY/MM/DD HH:mm:ss)
     */
    public static String getCurrentChinaTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss");
        return now.format(formatter);
    }
}