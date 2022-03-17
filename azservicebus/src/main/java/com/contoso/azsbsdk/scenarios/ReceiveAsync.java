package com.contoso.azsbsdk.scenarios;

import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.contoso.azsbsdk.util.Constants;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

@Service
public class ReceiveAsync extends ServiceBusScenario {
    private final ClientLogger LOGGER = new ClientLogger(ReceiveAsync.class);

    @Override
    public void run() {

        String connectionString = System.getenv(Constants.AZURE_SERVICEBUS_CONNECTION_STRING);
        String queueName = System.getenv(Constants.AZURE_SERVICEBUS_QUEUE_NAME);

        AmqpRetryOptions amqpRetryOptions = new AmqpRetryOptions();
        amqpRetryOptions.setMode(AmqpRetryMode.FIXED);
        amqpRetryOptions.setMaxDelay(Duration.ofSeconds(15));
        amqpRetryOptions.setMaxRetries(25);

        ServiceBusReceiverAsyncClient receiver = new ServiceBusClientBuilder()
                .retryOptions(amqpRetryOptions)
                .connectionString(connectionString)
                .receiver()
                .maxAutoLockRenewDuration(Duration.ZERO)
                .disableAutoComplete()
                .queueName(queueName)
                .buildAsyncClient();

        CountDownLatch latch = new CountDownLatch(1);

        receiver.receiveMessages()
                .log()
                .doOnSubscribe(s -> {
                    System.out.println("subscribed");
                })
                .subscribe(
                        event -> {
                            System.out.println("got-message:" + event.getMessageId());
                        } ,
                        throwable -> {
                            throwable.printStackTrace();
                            System.out.println("errored....");
                            latch.countDown();
                        },
                        () -> {
                            System.out.println("completed....");
                            latch.countDown();
                        });


        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
