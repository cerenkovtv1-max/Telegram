package org.telegram.ui.Gifts;

import static org.telegram.messenger.LocaleController.getString;

import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.util.List;
import java.util.Locale;

/**
 * "/soft https://t.me/nft/slug" ("you transferred a gift") and "/sotf https://t.me/nft/slug" ("the other person
 * transferred you a gift"): shows a local-only service message about a collectible gift transfer
 * in the open chat. The message has a negative id, is never stored and never sent anywhere, and no gift
 * is actually transferred. Its card is always drawn with the {@link #WATERMARK_TEXT} watermark
 * (see StarGiftUniqueActionLayout), there is no setting to turn that off.
 */
public class ParodyGiftMessage {

    public static final String WATERMARK_TEXT = "PARODY";

    /** Watermark width as a share of the card diagonal (0.5 = 50%). Tune it here. */
    public static final float WATERMARK_SIZE = 0.5f;
    /** Watermark opacity (0.28 = 28%). Tune it here. */
    public static final float WATERMARK_ALPHA = 0.28f;
    // the watermark is mandatory: whatever is set above, it never gets smaller or fainter than this
    public static final float WATERMARK_MIN_SIZE = 0.35f;
    public static final float WATERMARK_MIN_ALPHA = 0.15f;

    private static final String[] OUTGOING_COMMANDS = { "/soft ", "/софт " };
    private static final String[] INCOMING_COMMANDS = { "/sotf ", "/сотф " };

    private ParodyGiftMessage() {}

    public static class Command {
        /** false: "you transferred a gift", true: "<the other person> transferred you a gift" */
        public final boolean incoming;
        public final String link;

        private Command(boolean incoming, String link) {
            this.incoming = incoming;
            this.link = link;
        }
    }

    /** @return the parsed command, or null if the text is not a "/soft <link>" or "/sotf <link>" command */
    public static Command parseCommand(CharSequence text) {
        if (text == null) {
            return null;
        }
        final String trimmed = text.toString().trim();
        for (String command : OUTGOING_COMMANDS) {
            if (trimmed.regionMatches(true, 0, command, 0, command.length())) {
                return new Command(false, trimmed.substring(command.length()).trim());
            }
        }
        for (String command : INCOMING_COMMANDS) {
            if (trimmed.regionMatches(true, 0, command, 0, command.length())) {
                return new Command(true, trimmed.substring(command.length()).trim());
            }
        }
        return null;
    }

    /** @return slug of a https://t.me/nft/<slug> (or tg://nft?slug=<slug>) link, or null */
    public static String extractSlug(String link) {
        if (link == null) {
            return null;
        }
        String url = link.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        String slug = null;
        try {
            final Uri uri = Uri.parse(url);
            final String scheme = uri.getScheme();
            if ("tg".equalsIgnoreCase(scheme)) {
                if ("nft".equalsIgnoreCase(uri.getHost())) {
                    slug = uri.getQueryParameter("slug");
                }
            } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                String host = uri.getHost();
                host = host == null ? "" : host.toLowerCase(Locale.ROOT);
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                final List<String> segments = uri.getPathSegments();
                if (("t.me".equals(host) || "telegram.me".equals(host) || "telegram.dog".equals(host)) && segments.size() >= 2 && "nft".equalsIgnoreCase(segments.get(0))) {
                    slug = segments.get(1);
                }
            }
        } catch (Exception ignore) {
            return null;
        }
        if (slug == null || slug.isEmpty() || slug.length() > 128 || !slug.matches("[A-Za-z0-9_\\-]+")) {
            return null;
        }
        return slug;
    }

    /** Looks the gift up by slug (the same request the app uses to open t.me/nft links) and shows the local message in the chat. */
    public static void show(ChatActivity chat, Command command) {
        final String slug = extractSlug(command.link);
        if (slug == null) {
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.error, getString(R.string.UniqueGiftNotFound)).show();
            return;
        }
        final TLRPC.User user = chat.getCurrentUser();
        if (command.incoming && (user == null || user.self)) {
            // "transferred you a gift" needs a specific other person as the sender
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.error, getString(R.string.ParodyGiftNeedsPerson)).show();
            return;
        }
        final int currentAccount = chat.getCurrentAccount();
        final TL_stars.getUniqueStarGift req = new TL_stars.getUniqueStarGift();
        req.slug = slug;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            TL_stars.TL_starGiftUnique gift = null;
            if (res instanceof TL_stars.TL_payments_uniqueStarGift) {
                final TL_stars.TL_payments_uniqueStarGift r = (TL_stars.TL_payments_uniqueStarGift) res;
                MessagesController.getInstance(currentAccount).putUsers(r.users, false);
                MessagesController.getInstance(currentAccount).putChats(r.chats, false);
                if (r.gift instanceof TL_stars.TL_starGiftUnique) {
                    gift = (TL_stars.TL_starGiftUnique) r.gift;
                }
            }
            if (gift == null) {
                // gift data is not available (offline, burned, wrong link): show what the link itself tells us
                gift = createFallbackGift(slug);
            }
            if (chat.isFinished) {
                return;
            }
            final long peerId = user != null ? user.id : chat.getDialogId();
            if (!chat.addLocalMessage(create(currentAccount, peerId, gift, command.incoming))) {
                BulletinFactory.of(chat).createSimpleBulletin(R.raw.error, getString(R.string.ParodyGiftCantShow)).show();
            }
        }));
    }

    /**
     * Local service message with a unique-gift card, flagged as parody:
     * "You transferred a gift to ..." or, if incoming, "... transferred you a gift!".
     */
    public static MessageObject create(int currentAccount, long dialogId, TL_stars.TL_starGiftUnique gift, boolean incoming) {
        final MessagesController controller = MessagesController.getInstance(currentAccount);
        final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();

        final TLRPC.TL_messageActionStarGiftUnique action = new TLRPC.TL_messageActionStarGiftUnique();
        action.gift = gift;
        action.transferred = true;

        final TLRPC.TL_messageService message = new TLRPC.TL_messageService();
        message.local_id = message.id = UserConfig.getInstance(currentAccount).getNewMessageId();
        message.dialog_id = dialogId;
        message.out = !incoming;
        // like real private messages: the sender is in from_id, an incoming message is addressed to us
        message.from_id = controller.getPeer(incoming ? dialogId : selfId);
        message.peer_id = controller.getPeer(incoming ? selfId : dialogId);
        message.date = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        message.action = action;

        final MessageObject messageObject = new MessageObject(currentAccount, message, true, false);
        messageObject.isParodyGift = true;
        return messageObject;
    }

    private static TL_stars.TL_starGiftUnique createFallbackGift(String slug) {
        final TL_stars.TL_starGiftUnique gift = new TL_stars.TL_starGiftUnique();
        gift.slug = slug;
        gift.title = slug;
        final int dash = slug.lastIndexOf('-');
        if (dash > 0 && dash < slug.length() - 1) {
            try {
                gift.num = Integer.parseInt(slug.substring(dash + 1));
                gift.title = slug.substring(0, dash);
            } catch (NumberFormatException ignore) {}
        }
        return gift;
    }
}
