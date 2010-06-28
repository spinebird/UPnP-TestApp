/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.protocol.sync;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.gena.IncomingSubscribeResponseMessage;
import org.teleal.cling.model.message.gena.OutgoingRenewalRequestMessage;
import org.teleal.cling.protocol.SendingSync;

import java.util.logging.Logger;


public class SendingRenewal extends SendingSync<OutgoingRenewalRequestMessage, IncomingSubscribeResponseMessage> {

    protected static Logger log = Logger.getLogger(SendingRenewal.class.getName());

    final protected RemoteGENASubscription subscription;

    public SendingRenewal(UpnpService upnpService, RemoteGENASubscription subscription) {
        super(upnpService, new OutgoingRenewalRequestMessage(subscription));
        this.subscription = subscription;
    }

    protected IncomingSubscribeResponseMessage executeSync() {
        log.fine("Sending subscription renewal request: " + getInputMessage());

        StreamResponseMessage response = getUpnpService().getRouter().send(getInputMessage());

        if (response == null) {
            log.fine("Subscription renewal failed, no response received");
            getUpnpService().getRegistry().removeRemoteSubscription(subscription);
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.end(CancelReason.RENEWAL_FAILED, null);
                        }
                    }
            );
            return null;
        }

        final IncomingSubscribeResponseMessage responseMessage = new IncomingSubscribeResponseMessage(response);

        if (response.getOperation().isFailed()) {
            log.fine("Subscription renewal failed, response was: " + response);
            getUpnpService().getRegistry().removeRemoteSubscription(subscription);
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.end(CancelReason.RENEWAL_FAILED,responseMessage.getOperation());
                        }
                    }
            );
        } else {
            log.fine("Subscription renewed, updating in registry, response was: " + response);
            subscription.setActualSubscriptionDurationSeconds(responseMessage.getSubscriptionDurationSeconds());
            getUpnpService().getRegistry().updateRemoteSubscription(subscription);
        }

        return responseMessage;
    }
}