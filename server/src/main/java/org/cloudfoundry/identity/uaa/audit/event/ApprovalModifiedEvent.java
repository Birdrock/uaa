package org.cloudfoundry.identity.uaa.audit.event;

import org.cloudfoundry.identity.uaa.approval.Approval;
import org.cloudfoundry.identity.uaa.audit.AuditEvent;
import org.cloudfoundry.identity.uaa.audit.AuditEventType;
import org.cloudfoundry.identity.uaa.util.JsonUtils;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

public class ApprovalModifiedEvent extends AbstractUaaEvent {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public ApprovalModifiedEvent(Object source, Authentication authentication) {
    super(source, authentication, IdentityZoneHolder.getCurrentZoneId());
    if (!Approval.class.isAssignableFrom(source.getClass())) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public Approval getSource() {
    return (Approval) super.getSource();
  }

  @Override
  public AuditEvent getAuditEvent() {
    Approval source = getSource();
    return createAuditRecord(
        source.getUserId(),
        AuditEventType.ApprovalModifiedEvent,
        getOrigin(getAuthentication()),
        getData(source));
  }

  private String getData(Approval source) {
    try {
      return JsonUtils.writeValueAsString(new ApprovalModifiedEventData(source));
    } catch (JsonUtils.JsonUtilException e) {
      logger.error("error writing approval event data", e);
    }
    return null;
  }

  private static class ApprovalModifiedEventData {

    private String scope;
    private Approval.ApprovalStatus status;

    public ApprovalModifiedEventData(Approval approval) {
      scope = approval.getScope();
      status = approval.getStatus();
    }

    public String getScope() {
      return scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }

    public Approval.ApprovalStatus getStatus() {
      return status;
    }

    public void setStatus(Approval.ApprovalStatus status) {
      this.status = status;
    }
  }
}
