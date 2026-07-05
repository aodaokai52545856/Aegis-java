package com.aegis.general.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {
    @Tool(description = "查询当前城市的天气状态")
    public String getWeather(@ToolParam(description = "城市名称") String city){
        return city + "今日天气：晴，25℃，风力2级";
    }

}
