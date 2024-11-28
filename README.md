## Orin Ayo
Orin Ayo in Yoruba means "Song of Joy". 

This openfire plugin enables Openfire to be a live music collaborative platform that enables musical instruments to publish and subscribe to RTP-MIDI and Media Streams with XMPP. 

It uses :
- [XEP-0231: Bits of Binary](https://xmpp.org/extensions/xep-0231.html) to send and receive MIDI events over XMPP
- [Java RTP-Midi](https://github.com/LeovR/rtp-midi) to support wireless MIDI hardware over a LAN (WIFI)
- [Broadcast Box](https://github.com/Glimesh/broadcast-box) to implement a [payload format for publishing and subscribing to media streams in XMPP using WebRTC-based ingestion/egress](https://igniterealtime.github.io/openfire-orinayo-plugin/xep)

The MIDI events and the WebRTC SDP payload format are transported using IQ stanzas and the communication between devices is based on personal eventing protocol, a profile of XMPP publish-subscribe specified in [XEP-0163](https://xmpp.org/extensions/xep-0163.html).

It also includes the [OrinAyo Web Client](https://github.com/Jus-Be/orin-ayo) which supports digital Guitar Controller devices.

## CI Build Status

[![Build Status](https://github.com/igniterealtime/openfire-orinayo-plugin/workflows/Java%20CI/badge.svg)](https://github.com/igniterealtime/openfire-orinayo-plugin/actions)

## Overview

## Known Issues

This version has embedded binaries for only Linux 64 and Windows 64.

## Installation

copy orinayo.jar to the plugins folder

## Configuration


## How to use

## Further information




