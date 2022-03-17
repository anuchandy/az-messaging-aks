package com.contoso.azehsdk.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.contoso.azehsdk.util.Constants;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class ReceiveAsync extends EventHubsScenario {
    private final ClientLogger LOGGER = new ClientLogger(ReceiveAsync.class);

    @Override
    public void run() {
        String stgConnectionString = System.getenv(Constants.AZURE_STORAGE_CONN_STR);

        int i = 0;
        while(true) {
            System.out.println("run_" + i++ + "found connection string: " + stgConnectionString != null);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
