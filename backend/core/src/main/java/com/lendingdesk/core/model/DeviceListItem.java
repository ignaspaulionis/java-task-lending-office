package com.lendingdesk.core.model;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.DeviceStatus;

/** A device together with the employee currently holding it ({@code null} if nobody). */
public record DeviceListItem(Device device, Long loanedTo) {

  public boolean available() {
    return device.getStatus() == DeviceStatus.AVAILABLE && loanedTo == null;
  }
}
