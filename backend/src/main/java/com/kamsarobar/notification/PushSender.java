package com.kamsarobar.notification;

import java.util.List;
import java.util.Set;

/**
 * Delivers push notifications. The default implementation uses Expo's free push service (which talks to
 * Firebase / Apple for us); another provider can be plugged in by implementing this interface.
 */
public interface PushSender {

    /** Sends the messages and returns the tokens that are no longer valid (app uninstalled, etc.). */
    Set<String> send(List<PushMessage> messages);
}
