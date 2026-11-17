package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class NameSimilarityTest {

  @Test
  void ac01703JanezNovakAndJanezNovackScoreTheDiceOfTheirTrigrams() {
    // 8 shared of 9 and 10 trigrams: 2 * 8 / 19 = 0.84 (D-25 (a)).
    assertThat(NameSimilarity.of("janez novak", "janez novack")).isCloseTo(16.0 / 19, within(1e-9));
  }

  @Test
  void ex01706EqualNamesScoreOne() {
    assertThat(NameSimilarity.of("ziga cuk", "ziga cuk")).isEqualTo(1.0);
  }

  @Test
  void r01704NamesWithoutSharedTrigramsScoreZero() {
    assertThat(NameSimilarity.of("janez novak", "maja zupan")).isZero();
  }

  @Test
  void r01704NamesShorterThanOneTrigramScoreOneOnlyWhenEqual() {
    assertThat(NameSimilarity.of("jo", "jo")).isEqualTo(1.0);
    assertThat(NameSimilarity.of("jo", "ja")).isZero();
  }
}
