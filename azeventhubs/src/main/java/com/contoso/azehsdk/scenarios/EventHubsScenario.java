package com.contoso.azehsdk.scenarios;

import com.contoso.azehsdk.util.CmdlineArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public abstract class EventHubsScenario {
    @Autowired
    protected CmdlineArgs cmdlineArgs;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    private void postConstruct() {
    }

    public abstract void run();
}
