package app.contacts.service;

import java.util.List;

/**
 * A pair of Possible duplicates with every reason (contract {@code DuplicateCandidate}, R-017-01).
 *
 * @param first the Contact earlier in the Contact list order (D-25 (b))
 * @param second the other Contact
 * @param reasons {@code sameEmail}, {@code samePhone}, {@code similarNameSameCompany}, in that
 *     order
 */
public record DuplicateCandidateView(
    ContactRefView first, ContactRefView second, List<String> reasons) {

  /** Keeps its own unmodifiable copy of the reasons. */
  public DuplicateCandidateView {
    reasons = List.copyOf(reasons);
  }
}
