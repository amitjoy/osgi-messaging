/*******************************************************************************
 * Copyright 2020-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider;

import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_ID;
import static in.bytehue.messaging.mqtt5.api.MqttMessageConstants.MESSAGING_PROTOCOL;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_MANY_SUB;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_SINGLE_SUB;
import static in.bytehue.messaging.mqtt5.provider.MessageReplyToWhiteboardProvider.ReplyToSubDTO.Type.REPLY_TO_SUB;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.adaptTo;
import static in.bytehue.messaging.mqtt5.provider.helper.MessageHelper.prepareExceptionAsMessage;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.messaging.Features.REPLY_TO;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_FEATURE_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_NAME_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.MESSAGING_PROTOCOL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY;
import static org.osgi.service.messaging.MessageConstants.REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.propertytypes.MessagingFeature;
import org.osgi.service.messaging.replyto.ReplyToManySubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSingleSubscriptionHandler;
import org.osgi.service.messaging.replyto.ReplyToSubscriptionHandler;
import org.osgi.util.pushstream.PushStream;

import in.bytehue.messaging.mqtt5.provider.MessageSubscriptionRegistry.ExtendedSubscription;
import in.bytehue.messaging.mqtt5.provider.helper.FilterParser;
import in.bytehue.messaging.mqtt5.provider.helper.FilterParser.Expression;
import in.bytehue.messaging.mqtt5.provider.helper.SubscriptionAck;

@Component
@MessagingFeature(name = MESSAGING_ID, protocol = MESSAGING_PROTOCOL)
public final class MessageReplyToWhiteboardProvider {

	@Reference(service = LoggerFactory.class)
	private Logger logger;

	@Reference
	private ConverterAdapter converter;

	@Reference
	private MessagePublisherProvider publisher;

	@Reference
	private MessageSubscriptionProvider subscriber;

	@Reference
	private MessageSubscriptionRegistry registry;

	@Reference
	private ComponentServiceObjects<MessageContextBuilderProvider> mcbFactory;

	private final List<ReplyToSubDTO> subscriptions = new CopyOnWriteArrayList<>();

	@Activate
	void activate() {
		subscriptions.stream().filter(sub -> !sub.isProcessed()).forEach(sub -> {
			switch (sub.type) {
			case REPLY_TO_SUB:
				processReplyToSubscriptionHandler(sub);
				break;
			case REPLY_TO_SINGLE_SUB:
				processReplyToSingleSubscriptionHandler(sub);
				break;
			case REPLY_TO_MANY_SUB:
				processReplyToManySubscriptionHandler(sub);
				break;
			}
		});
	}

	@Deactivate
	void deactivate() {
		subscriptions.stream().forEach(sub -> sub.subAcks.stream().forEach(s -> s.stream().close()));
		subscriptions.clear();
	}

	@Reference(policy = DYNAMIC, cardinality = MULTIPLE)
	synchronized void addReplyToSingleSubscriptionHandler(final ReplyToSingleSubscriptionHandler handler,
			final ServiceReference<?> reference) {

		final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_SINGLE_SUB, reference);
		subscriptions.add(sub);

		if (converter == null || subscriber == null || registry == null) {
			// may happen that the services ain't injected yet
			return;
		}
		processReplyToSingleSubscriptionHandler(sub);
	}

	synchronized void removeReplyToSingleSubscriptionHandler(final ServiceReference<?> reference) {
		removeSubscription(reference);
	}

	@Reference(policy = DYNAMIC, cardinality = MULTIPLE)
	synchronized void addReplyToSubscriptionHandler(final ReplyToSubscriptionHandler handler,
			final ServiceReference<?> reference) {

		final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_SUB, reference);
		subscriptions.add(sub);

		if (converter == null || subscriber == null || registry == null) {
			// may happen that the services ain't injected yet
			return;
		}
		processReplyToSubscriptionHandler(sub);
	}

	synchronized void removeReplyToSubscriptionHandler(final ServiceReference<?> reference) {
		removeSubscription(reference);
	}

	@Reference(policy = DYNAMIC, cardinality = MULTIPLE)
	synchronized void addReplyToManySubscriptionHandler(final ReplyToManySubscriptionHandler handler,
			final ServiceReference<?> reference) {

		final ReplyToSubDTO sub = new ReplyToSubDTO(handler, REPLY_TO_MANY_SUB, reference);
		subscriptions.add(sub);

		if (converter == null || subscriber == null || registry == null) {
			// may happen that the services ain't injected yet
			return;
		}
		processReplyToManySubscriptionHandler(sub);
	}

	synchronized void removeReplyToManySubscriptionHandler(final ServiceReference<?> reference) {
		removeSubscription(reference);
	}

	private void processReplyToSingleSubscriptionHandler(final ReplyToSubDTO sub) {
		final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

		Stream.of(replyToDTO.subChannels).forEach(c -> {
			final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel);
			sub.addAck(ack);

			ack.stream().map(m -> handleResponse(m, (ReplyToSingleSubscriptionHandler) sub.handler))
					.forEach(m -> handleMessageReceive(sub.reference, replyToDTO, c, ack, m));
		});
	}

	private void processReplyToSubscriptionHandler(final ReplyToSubDTO sub) {
		final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

		Stream.of(replyToDTO.subChannels).forEach(c -> {
			final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel);
			sub.addAck(ack);

			ack.stream().forEach(((ReplyToSubscriptionHandler) sub.handler)::handleResponse);
		});
	}

	private void processReplyToManySubscriptionHandler(final ReplyToSubDTO sub) {
		final ReplyToDTO replyToDTO = new ReplyToDTO(sub.reference);

		Stream.of(replyToDTO.subChannels).forEach(c -> {
			final SubscriptionAck ack = subscriber.replyToSubscribe(c, replyToDTO.pubChannel);
			sub.addAck(ack);

			ack.stream().forEach(m -> handleResponses(m, (ReplyToManySubscriptionHandler) sub.handler)
					.forEach(msg -> handleMessageReceive(sub.reference, replyToDTO, c, ack, msg)));
		});
	}

	private Message handleResponse(final Message request, final ReplyToSingleSubscriptionHandler handler) {
		final MessageContextBuilderProvider mcb = getResponse(request);
		try {
			return handler.handleResponse(request, mcb);
		} catch (final Exception e) {
			return prepareExceptionAsMessage(e, mcb);
		} finally {
			mcbFactory.ungetService(mcb);
		}
	}

	private MessageContextBuilderProvider getResponse(final Message request) {
		final MessageContext context = request.getContext();
		final String channel = context.getChannel();
		final String replyToChannel = context.getReplyToChannel();
		final String correlation = context.getCorrelationId();

		return (MessageContextBuilderProvider) mcbFactory.getService().channel(channel).replyTo(replyToChannel)
				.correlationId(correlation).content(request.payload());
	}

	private PushStream<Message> handleResponses(final Message request, final ReplyToManySubscriptionHandler handler) {
		final MessageContextBuilder mcb = getResponse(request);
		return handler.handleResponses(request, mcb);
	}

	private void handleMessageReceive(final ServiceReference<?> reference, final ReplyToDTO replyToDTO,
			final String channel, final SubscriptionAck sub, final Message msg) {

		final String pubChannelProp = replyToDTO.pubChannel;
		final String pubChannel = pubChannelProp == null ? msg.getContext().getReplyToChannel() : pubChannelProp;

		if (pubChannel == null) {
			logger.warn("No reply to channel is specified for the subscription handler");
			return;
		}
		// update the subscription
		final ExtendedSubscription subscription = registry.getSubscription(channel, sub.id());
		subscription.updateReplyToHandlerSubscription(pubChannel, reference);

		publisher.publish(msg, pubChannel);
	}

	private class ReplyToDTO {

		boolean isConform;
		String pubChannel;
		String[] subChannels;

		ReplyToDTO(final ServiceReference<?> reference) {
			final Dictionary<String, ?> properties = reference.getProperties();

			final Object replyToSubResponse = properties.get(REPLY_TO_SUBSCRIPTION_RESPONSE_CHANNEL_PROPERTY);
			final Object replyToSubRequest = properties.get(REPLY_TO_SUBSCRIPTION_REQUEST_CHANNEL_PROPERTY);

			pubChannel = adaptTo(replyToSubResponse, String.class, converter);
			subChannels = adaptTo(replyToSubRequest, String[].class, converter);

			if (subChannels == null) {
				throw new IllegalStateException("The '" + reference
						+ "' handler instance doesn't specify the reply-to subscription channel(s)");
			}

			final Object replyToSubTgt = properties.get(REPLY_TO_SUBSCRIPTION_TARGET_PROPERTY);
			final String replyToSubTarget = adaptTo(replyToSubTgt, String.class, converter);

			final FilterParser fp = new FilterParser();
			final Expression exp = fp.parse(replyToSubTarget);

			final Map<String, String> requiredValues = new HashMap<>();

			requiredValues.put(MESSAGING_FEATURE_PROPERTY, REPLY_TO);
			requiredValues.put(MESSAGING_NAME_PROPERTY, MESSAGING_ID);
			requiredValues.put(MESSAGING_PROTOCOL_PROPERTY, MESSAGING_PROTOCOL);

			isConform = exp.eval(requiredValues);

			if (!isConform) {
				throw new IllegalStateException(
						"The '" + reference + "' handler service doesn't specify the reply-to target filter");
			}
		}
	}

	static class ReplyToSubDTO {

		enum Type {
			REPLY_TO_SUB, REPLY_TO_SINGLE_SUB, REPLY_TO_MANY_SUB
		}

		Type type;
		Object handler;
		ServiceReference<?> reference;
		List<SubscriptionAck> subAcks = new ArrayList<>();

		public ReplyToSubDTO(final Object handler, final Type type, final ServiceReference<?> reference) {
			this.handler = handler;
			this.type = type;
			this.reference = reference;
		}

		public synchronized void addAck(final SubscriptionAck subAck) {
			subAcks.add(subAck);
		}

		public synchronized boolean isProcessed() {
			return !subAcks.isEmpty();
		}

	}

	private synchronized void removeSubscription(final ServiceReference<?> reference) {
		subscriptions.stream().filter(sub -> sub.reference == reference)
				.forEach(sub -> sub.subAcks.stream().forEach(s -> s.stream().close()));
		subscriptions.removeIf(sub -> sub.reference == reference);
	}

}
