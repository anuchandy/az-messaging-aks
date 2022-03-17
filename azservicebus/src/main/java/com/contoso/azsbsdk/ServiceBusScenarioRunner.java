package com.contoso.azsbsdk;

import com.contoso.azsbsdk.scenarios.ServiceBusScenario;
import com.contoso.azsbsdk.util.CmdlineArgs;
import com.contoso.azsbsdk.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class ServiceBusScenarioRunner implements ApplicationRunner {

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected CmdlineArgs cmdlineArgs;

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusScenarioRunner.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String scenarioName = cmdlineArgs.get(Constants.SCENARIO_NAME);
        if (scenarioName != null) {
            scenarioName = "com.contoso.azsbsdk.scenarios." + scenarioName;
        }
        ServiceBusScenario scenario = (ServiceBusScenario) applicationContext.getBean(Class.forName(scenarioName));
        scenario.run();
    }
}
