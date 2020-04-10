package org.cloudfoundry.identity.uaa.scim.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.cloudfoundry.identity.uaa.audit.AuditEvent;
import org.cloudfoundry.identity.uaa.audit.AuditEventType;
import org.cloudfoundry.identity.uaa.audit.event.AbstractUaaEvent;
import org.cloudfoundry.identity.uaa.util.JsonUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class GroupModifiedEvent extends AbstractUaaEvent {

  private String groupId;
  private String groupName;
  private String[] members;
  private AuditEventType eventType;

  protected GroupModifiedEvent(
      String groupId,
      String name,
      String[] members,
      AuditEventType type,
      Authentication authentication,
      String zoneId) {
    super(authentication, zoneId);
    this.groupId = groupId;
    this.groupName = name;
    this.members = members;
    this.eventType = type;
  }

  public static GroupModifiedEvent groupCreated(
      String group, String name, String[] members, String zoneId) {
    return new GroupModifiedEvent(
        group, name, members, AuditEventType.GroupCreatedEvent, getContextAuthentication(), zoneId);
  }

  public static GroupModifiedEvent groupModified(
      String group, String name, String[] members, String zoneId) {
    return new GroupModifiedEvent(
        group,
        name,
        members,
        AuditEventType.GroupModifiedEvent,
        getContextAuthentication(),
        zoneId);
  }

  public static GroupModifiedEvent groupDeleted(
      String group, String name, String[] members, String zoneId) {
    return new GroupModifiedEvent(
        group, name, members, AuditEventType.GroupDeletedEvent, getContextAuthentication(), zoneId);
  }

  protected static Authentication getContextAuthentication() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) {
      a =
          new Authentication() {
            ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
              return authorities;
            }

            @Override
            public Object getCredentials() {
              return null;
            }

            @Override
            public Object getDetails() {
              return null;
            }

            @Override
            public Object getPrincipal() {
              return "null";
            }

            @Override
            public boolean isAuthenticated() {
              return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

            @Override
            public String getName() {
              return "null";
            }
          };
    }
    return a;
  }

  @Override
  public AuditEvent getAuditEvent() {
    String data = JsonUtils.writeValueAsString(new GroupInfo(groupName, members));
    return createAuditRecord(groupId, eventType, getOrigin(getAuthentication()), data);
  }

  public String getGroupId() {
    return groupId;
  }

  public String[] getMembers() {
    return members;
  }

  public static class GroupInfo {

    @JsonIgnore private String group;
    @JsonIgnore private String[] members;

    @JsonCreator
    public GroupInfo(@JsonProperty("group_name") String g, @JsonProperty("members") String[] m) {
      this.group = g;
      this.members = m;
      // sort for equals() to work
      Arrays.sort(members);
    }

    @JsonProperty("group_name")
    public String getGroup() {
      return group;
    }

    @JsonProperty("members")
    public String[] getMembers() {
      return members;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      GroupInfo groupInfo = (GroupInfo) o;

      if (!group.equals(groupInfo.group)) {
        return false;
      }
      if (!Arrays.equals(members, groupInfo.members)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = group.hashCode();
      result = 31 * result + Arrays.hashCode(members);
      return result;
    }

    @Override
    public String toString() {
      return "GroupInfo{"
          + "group='"
          + group
          + '\''
          + ", members="
          + Arrays.toString(members)
          + '}';
    }
  }
}
