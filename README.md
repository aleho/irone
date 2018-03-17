# IronE

Notifications for Nokia Steel HR.


## About

This app tries to extend notifications for the Nokia Steel HR to be received from any application.


# Findings

Poking around logs and Bluetooth protocol captures I was able to gather some information on device
communication and BtLE services.


## Services

<table>
  <thead>
    <tr>
      <th colspan="3">Type</th>
      <th>UUID</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td colspan="3">Service</td>
      <td><pre>00001800-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_access.xml">Generic access</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00002a00-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.device_name.xml">Device name</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00002a01-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.appearance.xml">Appearance</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00002a04-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters.xml">Peripheral Preferred Connection Parameters</a></td>
    </tr>
    <tr><td colspan="5"></td></tr>
    <tr>
      <td colspan="3">Service</td>
      <td><pre>00001801-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_attribute.xml">Generic attribute</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00002a05-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.gatt.service_changed.xml">Service Changed</a></td>
    </tr>
    <tr>
      <td></td>
      <td></td>
      <td>Descriptor</td>
      <td><pre>00002902-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml">Client Characteristic Configuration</a></td>
    </tr>
    <tr><td colspan="5"></td></tr>
    <tr>
      <td colspan="3">Service</td>
      <td><pre>00000020-5749-5448-0037-000000000000</pre></td>
      <td>Proprietary</td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00000021-5749-5448-0037-000000000000</pre></td>
      <td>Proprietary</td>
    </tr>
    <tr>
      <td></td>
      <td></td>
      <td>Descriptor</td>
      <td><pre>00002902-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml">Client Characteristic Configuration</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00000022-5749-5448-0037-000000000000</pre></td>
      <td>Proprietary</td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00000023-5749-5448-0037-000000000000</pre></td>
      <td>Proprietary</td>
    </tr>
    <tr>
      <td></td>
      <td></td>
      <td>Descriptor</td>
      <td><pre>00002902-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml">Client Characteristic Configuration</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00000024-5749-5448-0037-000000000000</pre></td>
      <td>Proprietary</td>
    </tr>
    <tr>
      <td></td>
      <td></td>
      <td>Descriptor</td>
      <td><pre>00002902-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml">Client Characteristic Configuration</a></td>
    </tr>
  </tbody>
</table>

Judging by my observations, `00000023-5749-5448-0037-000000000000` seems to be used for various
kinds of device setup and communication. For all other commands except for "show me device
information" (which seems to return a MAC-Address), all other commands (e.g. "enable notifications",
"tell me your current notification setup") require authentication by the client using a
challenge-response procedure with a secret that was probably configured during initial device
pairing and setup.


## Gatt Server

<table>
  <thead>
    <tr>
      <th colspan="3">Type</th>
      <th>UUID</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td colspan="3">Service</td>
      <td><pre>00001811-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.alert_notification.xml">Alert Notification Service</a></td>
    </tr>
    <tr>
      <td></td>
      <td colspan="2">Characteristic</td>
      <td><pre>00002A46-0000-1000-8000-00805f9b34fb</pre></td>
      <td><a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.new_alert.xml">New Alert</a></td>
    </tr>
  </tbody>
</table>

To send a notification to the device `00002A46-0000-1000-8000-00805f9b34fb` has to be provided as a
GATT server by the app which the watch will connect to.
