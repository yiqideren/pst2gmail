package com.atsid.exchange.email;

import com.google.common.base.Preconditions;
import com.pff.PSTMessage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SenderResolver {
    @Value("${sender.resolver.map}")
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Map<String, String> resolverMap;

    /**
     * Resolves the senders email address for addresses that are in an exchange format instead of SMTP.
     *
     * @param pstMessage Email message from PST to extract an exchanged based sender from.
     * @return
     */
    public String resolveSender(PSTMessage pstMessage) {
        Preconditions.checkState(resolverMap != null, "resolverMap has not been instantiated");
        Preconditions.checkArgument(pstMessage != null, "pstMessage cannot be null");

        if ("EX".equalsIgnoreCase(pstMessage.getSenderAddrtype())) {
            for (Map.Entry<String, String> entry : resolverMap.entrySet()) {
                String senderEmailAddress = pstMessage.getSenderEmailAddress();

                if (senderEmailAddress.startsWith(entry.getKey())) {
                    String user = senderEmailAddress.substring(senderEmailAddress.lastIndexOf("=") + 1);

                    return user + entry.getValue();
                }
            }
        } else {
            throw new IllegalStateException("Cannot convert a non-exchange formatted sender address");
        }

        return "";
    }
}