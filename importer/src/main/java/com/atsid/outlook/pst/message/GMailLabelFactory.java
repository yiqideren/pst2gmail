package com.atsid.outlook.pst.message;

import com.google.api.services.gmail.Gmail;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for obtaining <code>GMailLabeler</code> instances.
 */
@Component
@Log4j
public class GMailLabelFactory implements ApplicationContextAware {
    @Setter
    private ApplicationContext applicationContext;
    @Getter(AccessLevel.PROTECTED)
    private Map<String, GMailLabeler> labelers = new HashMap<>();

    /**
     * Creates a <code>GMailLabeler</code> instance and returns it after initialization.
     * Multiple calls with the same email address will return the same instance using the original <code>Gmail</code> service..
     *
     * @param gmailService Gmail service object to use
     * @param emailAddress Email address to associate with this labeler
     * @return Returns an initialized labeler or null if initialization fails.
     */
    public GMailLabeler getLabeler(Gmail gmailService, String emailAddress) {
        if (labelers.containsKey(emailAddress)) {
            return labelers.get(emailAddress);
        } else {
            GMailLabeler labeler =
                    (GMailLabeler) applicationContext.getBean("gmailLabeler", gmailService, emailAddress);

            try {
                labeler.loadLabels();
            } catch (IOException ioe) {
                log.error(String.format("Could not create gmail labeler for %s due to an IOException.", emailAddress),
                        ioe);
                return null;
            }

            labelers.put(emailAddress, labeler);

            return labeler;
        }
    }
}