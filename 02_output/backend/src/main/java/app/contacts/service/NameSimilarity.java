package app.contacts.service;

import java.util.HashSet;
import java.util.Set;

/**
 * Name similarity of two Normalised full names: the Sørensen–Dice coefficient of their sets of
 * character trigrams, from 0 to 1 (US-017 Definitions, D-25 (a), ADR-12).
 */
final class NameSimilarity {

  private NameSimilarity() {}

  /**
   * The Name similarity.
   *
   * @param first a Normalised full name
   * @param second another Normalised full name
   * @return 1 for equal names, else twice the shared trigrams over the sum of both trigram counts
   */
  static double of(String first, String second) {
    if (first.equals(second)) {
      return 1.0;
    }
    Set<String> firstTrigrams = trigrams(first);
    Set<String> secondTrigrams = trigrams(second);
    int total = firstTrigrams.size() + secondTrigrams.size();
    if (total == 0) {
      return 0.0;
    }
    Set<String> shared = new HashSet<>(firstTrigrams);
    shared.retainAll(secondTrigrams);
    return 2.0 * shared.size() / total;
  }

  private static Set<String> trigrams(String name) {
    Set<String> trigrams = new HashSet<>();
    for (int start = 0; start + 3 <= name.length(); start++) {
      trigrams.add(name.substring(start, start + 3));
    }
    return trigrams;
  }
}
