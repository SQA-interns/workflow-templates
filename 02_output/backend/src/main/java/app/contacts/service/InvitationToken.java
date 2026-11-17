package app.contacts.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * The token at the end of an Invitation link: the Group id and 128 random bits, base64url (SR-05,
 * D-63). The Group id lets every lookup stay inside one Group (AR-04); only the SHA-256 hash of the
 * whole token is stored.
 *
 * @param groupId the Group the Invitation belongs to
 * @param value the token as it appears in the link
 */
public record InvitationToken(UUID groupId, String value) {

  private static final int RANDOM_BYTES = 16;

  /** 16 bytes of the Group id plus 16 random bytes, base64url without padding. */
  private static final int LENGTH = 44;

  private static final int GROUP_PART = 22;

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  /**
   * A new token for an Invitation of the Group.
   *
   * @param groupId the Group
   * @return the token
   */
  static InvitationToken issue(UUID groupId) {
    byte[] random = new byte[RANDOM_BYTES];
    RANDOM.nextBytes(random);
    return new InvitationToken(groupId, encode(groupId) + ENCODER.encodeToString(random));
  }

  /**
   * The token of a link, if it has the form of an issued one (EX-004-08).
   *
   * @param value the token from the link
   * @return the token, empty when it cannot have been issued
   */
  static Optional<InvitationToken> parse(String value) {
    if (value == null
        || value.length() != LENGTH
        || !value.chars().allMatch(InvitationToken::isBase64Url)) {
      return Optional.empty();
    }
    ByteBuffer group =
        ByteBuffer.wrap(Base64.getUrlDecoder().decode(value.substring(0, GROUP_PART)));
    UUID groupId = new UUID(group.getLong(), group.getLong());
    return encode(groupId).equals(value.substring(0, GROUP_PART))
        ? Optional.of(new InvitationToken(groupId, value))
        : Optional.empty();
  }

  /**
   * The SHA-256 hash of the token, the only form that is stored (SR-05).
   *
   * @return 32 bytes
   */
  byte[] hash() {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException("SHA-256 is part of every Java runtime", missing);
    }
  }

  private static String encode(UUID groupId) {
    ByteBuffer bytes = ByteBuffer.allocate(RANDOM_BYTES);
    bytes.putLong(groupId.getMostSignificantBits()).putLong(groupId.getLeastSignificantBits());
    return ENCODER.encodeToString(bytes.array());
  }

  private static boolean isBase64Url(int c) {
    return c >= 'A' && c <= 'Z'
        || c >= 'a' && c <= 'z'
        || c >= '0' && c <= '9'
        || c == '-'
        || c == '_';
  }
}
