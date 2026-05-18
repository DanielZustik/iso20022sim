package cz.monetplus.iso20022sim.service;

import org.springframework.stereotype.Service;

@Service
public class ApprovalDecisionService {

    public boolean isApprovedByDefault() {
        return true;
    }
}
