package com.contoso.azehsdk;

import com.contoso.azehsdk.scenarios.EventHubsScenario;
import com.contoso.azehsdk.util.CmdlineArgs;
import com.contoso.azehsdk.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class EventHubsScenarioRunner implements ApplicationRunner {

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected CmdlineArgs cmdlineArgs;

    public static void main(String[] args) {
        SpringApplication.run(EventHubsScenarioRunner.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String scenarioName = cmdlineArgs.get(Constants.SCENARIO_NAME);
        if (scenarioName != null) {
            scenarioName = "com.contoso.azehsdk.scenarios." + scenarioName;
        }
        EventHubsScenario scenario = (EventHubsScenario) applicationContext.getBean(Class.forName(scenarioName));
        scenario.run();
    }
}
