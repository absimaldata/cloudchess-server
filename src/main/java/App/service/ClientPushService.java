package App.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Log4j2
public class ClientPushService {

    @Autowired
    private AmazonSQSAsync amazonSQSAsync;

    @Value("${cloud.aws.end-point.uri}")
    private String queueName;

    public void pushToClient(String line) {
        try {
            SendMessageRequest sendMessageRequest = new SendMessageRequest();
            sendMessageRequest.setQueueUrl(queueName);
            sendMessageRequest.setMessageBody(line);
            sendMessageRequest.setMessageGroupId("analysis");
            sendMessageRequest.setMessageDeduplicationId(String.valueOf(System.nanoTime()) + UUID.randomUUID());
            amazonSQSAsync.sendMessage(sendMessageRequest);
            log.info("Message sent successfully. " + line);
        } catch (Exception e) {
            log.error("SQS Queue is not ready to accept line: " + line, e);
        }
    }
}