package fi.vm.sade.ryhmasahkoposti.externalinterface.component;

import fi.vm.sade.ryhmasahkoposti.api.dto.EmailBounces;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.BounceResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BounceComponent {
    @Resource
    private BounceResource bounceResourceClient;

    @Value("${ryhmasahkoposti.bounces.path}")
    private String bouncesPath;

    public EmailBounces getBounces() {
        return bounceResourceClient.getBounces(bouncesPath);
    }
}
